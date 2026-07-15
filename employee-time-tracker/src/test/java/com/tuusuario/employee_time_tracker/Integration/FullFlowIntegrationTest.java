package com.tuusuario.employee_time_tracker.Integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo completo contra la app real (H2 + Flyway + seguridad):
 * login -> alta de empleado -> PIN -> fichaje por kiosco -> break -> salida.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FullFlowIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private static String adminToken;
    private static String adminRefresh;
    private static String kioskToken;
    private static Long employeeId;
    private static Long manualEntryId;
    private static String manualEntryDate;

    private String login(String username, String password) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();
        JsonNode body = objectMapper.readTree(res.getResponse().getContentAsString());
        if (username.equals("admin")) {
            adminRefresh = body.get("refreshToken").asText();
        }
        return body.get("token").asText();
    }

    @Test
    @Order(1)
    void adminAndKioskSeedUsersCanLogin() throws Exception {
        adminToken = login("admin", "admin1234");
        kioskToken = login("kiosk", "kiosk1234");
        assertThat(adminToken).isNotBlank();
        assertThat(kioskToken).isNotBlank();
    }

    @Test
    @Order(2)
    void anonymousAndKioskCannotManageEmployees() throws Exception {
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(get("/api/employees")
                        .header("Authorization", "Bearer " + kioskToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(3)
    void adminCreatesEmployeeAndSetsPin() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Mica\",\"lastName\":\"Gomez\"," +
                                "\"email\":\"mica@test.com\",\"position\":\"Caja\"}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        employeeId = objectMapper.readTree(res.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(put("/api/employees/" + employeeId + "/pin")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"pin\":\"4321\"}"))
                .andExpect(status().is2xxSuccessful());

        // Email duplicado -> 409
        mockMvc.perform(post("/api/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Otra\",\"lastName\":\"Persona\"," +
                                "\"email\":\"mica@test.com\",\"position\":\"Caja\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(4)
    void kioskFullShiftFlow() throws Exception {
        String action = "{\"employeeId\":" + employeeId + ",\"pin\":\"4321\"}";
        String wrongPin = "{\"employeeId\":" + employeeId + ",\"pin\":\"9999\"}";

        // PIN incorrecto -> 401
        mockMvc.perform(post("/api/kiosk/verify")
                        .header("Authorization", "Bearer " + kioskToken)
                        .contentType(APPLICATION_JSON).content(wrongPin))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/kiosk/clock-in")
                        .header("Authorization", "Bearer " + kioskToken)
                        .contentType(APPLICATION_JSON).content(action))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("CLOCKED_IN"));

        // Doble clock-in -> 409
        mockMvc.perform(post("/api/kiosk/clock-in")
                        .header("Authorization", "Bearer " + kioskToken)
                        .contentType(APPLICATION_JSON).content(action))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/kiosk/break/start")
                        .header("Authorization", "Bearer " + kioskToken)
                        .contentType(APPLICATION_JSON).content(action))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("ON_BREAK"));

        // No se puede salir estando en break -> 409
        mockMvc.perform(post("/api/kiosk/clock-out")
                        .header("Authorization", "Bearer " + kioskToken)
                        .contentType(APPLICATION_JSON).content(action))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/kiosk/break/end")
                        .header("Authorization", "Bearer " + kioskToken)
                        .contentType(APPLICATION_JSON).content(action))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("CLOCKED_IN"));

        mockMvc.perform(post("/api/kiosk/clock-out")
                        .header("Authorization", "Bearer " + kioskToken)
                        .contentType(APPLICATION_JSON).content(action))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("NO_SHIFT"));
    }

    @Test
    @Order(5)
    void adminSeesReportsAndAuditLog() throws Exception {
        mockMvc.perform(get("/api/analytics/weekly-report")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalWorkedMinutes").exists());

        mockMvc.perform(get("/api/analytics/employees/" + employeeId + "/entries")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].employeeId").value(employeeId));

        // Sin datos -> lista vacia, no 404
        mockMvc.perform(get("/api/analytics/active-breaks")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/analytics/audit-log")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(6)
    void manualEntryPaidDoubleAndPayroll() throws Exception {
        // Cargar valor hora al empleado
        mockMvc.perform(put("/api/employees/" + employeeId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Mica\",\"lastName\":\"Gomez\"," +
                                "\"email\":\"mica@test.com\",\"position\":\"Caja\"," +
                                "\"hourlyRate\":5500}"))
                .andExpect(status().is2xxSuccessful());

        // Alta manual de una jornada olvidada de ayer (8 hs)
        String yesterday = java.time.LocalDate.now().minusDays(1).toString();
        MvcResult res = mockMvc.perform(post("/api/time-entries")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"employeeId\":" + employeeId + "," +
                                "\"clockIn\":\"" + yesterday + "T09:00:00\"," +
                                "\"clockOut\":\"" + yesterday + "T17:00:00\"}"))
                .andExpect(status().isOk())
                .andReturn();
        long entryId = objectMapper.readTree(res.getResponse().getContentAsString())
                .get("timeEntryId").asLong();
        manualEntryId = entryId;
        manualEntryDate = yesterday;

        // Marcarla como pagada doble (feriado)
        mockMvc.perform(patch("/api/time-entries/" + entryId + "/paid-double")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"paidDouble\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paidDouble").value(true));

        // La liquidacion de ayer refleja 8 hs dobles = 16 hs x $5500 = $88.000
        mockMvc.perform(get("/api/analytics/payroll?from=" + yesterday + "&to=" + yesterday)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].workedMinutes").value(480))
                .andExpect(jsonPath("$.rows[0].doubleMinutes").value(480))
                .andExpect(jsonPath("$.rows[0].payableMinutes").value(960))
                .andExpect(jsonPath("$.rows[0].amount").value(88000.00))
                .andExpect(jsonPath("$.totalAmount").value(88000.00));
    }

    @Test
    @Order(7)
    void paymentClosesPeriodAndReopeningUnlocksIt() throws Exception {
        // Pagar el periodo de la jornada manual (ayer)
        MvcResult res = mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"employeeId\":" + employeeId + "," +
                                "\"from\":\"" + manualEntryDate + "\"," +
                                "\"to\":\"" + manualEntryDate + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(88000.00))
                .andReturn();
        long paymentId = objectMapper.readTree(res.getResponse().getContentAsString())
                .get("id").asLong();

        // Pago duplicado del mismo periodo -> 409
        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"employeeId\":" + employeeId + "," +
                                "\"from\":\"" + manualEntryDate + "\"," +
                                "\"to\":\"" + manualEntryDate + "\"}"))
                .andExpect(status().isConflict());

        // El periodo queda bloqueado: editar la jornada -> 409
        mockMvc.perform(put("/api/time-entries/" + manualEntryId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"clockIn\":\"" + manualEntryDate + "T08:00:00\"," +
                                "\"clockOut\":\"" + manualEntryDate + "T16:00:00\"}"))
                .andExpect(status().isConflict());

        // La liquidacion muestra el pago
        mockMvc.perform(get("/api/analytics/payroll?from=" + manualEntryDate
                        + "&to=" + manualEntryDate)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].paymentId").value(paymentId));

        // El mensaje de WhatsApp tiene el desglose y el total
        MvcResult msg = mockMvc.perform(get("/api/analytics/payroll/" + employeeId
                        + "/message?from=" + manualEntryDate + "&to=" + manualEntryDate)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        String text = msg.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        org.assertj.core.api.Assertions.assertThat(text)
                .contains("Mica Gomez")
                .contains("FERIADO ×2")
                .contains("TOTAL: $88.000");

        // Reabrir el pago -> la jornada vuelve a ser editable
        mockMvc.perform(delete("/api/payments/" + paymentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(put("/api/time-entries/" + manualEntryId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"clockIn\":\"" + manualEntryDate + "T09:00:00\"," +
                                "\"clockOut\":\"" + manualEntryDate + "T17:00:00\"}"))
                .andExpect(status().isOk());

        // Sin jornadas auto-cerradas pendientes
        mockMvc.perform(get("/api/analytics/pending-fixes")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(8)
    void refreshTokenRotates() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + adminRefresh + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        // El refresh token usado queda revocado.
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + adminRefresh + "\"}"))
                .andExpect(status().isUnauthorized());

        adminRefresh = objectMapper.readTree(res.getResponse().getContentAsString())
                .get("refreshToken").asText();
    }
}
