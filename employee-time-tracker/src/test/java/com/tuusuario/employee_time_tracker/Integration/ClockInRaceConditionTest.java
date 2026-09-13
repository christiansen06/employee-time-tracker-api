package com.tuusuario.employee_time_tracker.Integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reproduce el caso real: un empleado ansioso toca "Entrada" (o "Iniciar
 * break") varias veces casi al mismo tiempo. Antes del indice unico de
 * V9__prevent_duplicate_open_entries.sql, dos pedidos que llegaban casi
 * juntos pasaban el chequeo "existsBy..." los dos antes de que cualquiera
 * guardara, y quedaban dos jornadas (o dos breaks) abiertas para la misma
 * persona. El freeze del boton en el frontend evita esto en el uso normal,
 * pero esta es la garantia real: sin importar cuantos pedidos lleguen
 * pegados, solo uno puede ganar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClockInRaceConditionTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    private String login(String username, String password) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString())
                .get("token").asText();
    }

    private long createEmployeeWithPin(String adminToken, String email, String pin) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/employees")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Race\",\"lastName\":\"Test\"," +
                                "\"email\":\"" + email + "\",\"position\":\"Caja\"}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        long id = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/employees/" + id + "/pin")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"pin\":\"" + pin + "\"}"))
                .andExpect(status().is2xxSuccessful());
        return id;
    }

    /** Dispara N pedidos identicos lo mas juntos posible con una barrera. */
    private List<Integer> fireConcurrently(String path, String body, String token, int times)
            throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(times);
        CyclicBarrier barrier = new CyclicBarrier(times);
        try {
            List<Callable<Integer>> tasks = java.util.stream.IntStream.range(0, times)
                    .<Callable<Integer>>mapToObj(i -> () -> {
                        barrier.await();
                        return mockMvc.perform(post(path)
                                        .header("Authorization", "Bearer " + token)
                                        .contentType(APPLICATION_JSON)
                                        .content(body))
                                .andReturn().getResponse().getStatus();
                    })
                    .toList();
            List<Future<Integer>> futures = pool.invokeAll(tasks, 10, TimeUnit.SECONDS);
            return futures.stream().map(f -> {
                try {
                    return f.get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).toList();
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void onlyOneOfManySimultaneousClockInsSucceeds() throws Exception {
        String adminToken = login("admin", "admin1234");
        String kioskToken = login("kiosk", "kiosk1234");
        long employeeId = createEmployeeWithPin(adminToken, "race-clockin@test.com", "1111");

        String action = "{\"employeeId\":" + employeeId + ",\"pin\":\"1111\"}";
        List<Integer> statuses = fireConcurrently(
                "/api/kiosk/clock-in", action, kioskToken, 5);

        assertThat(statuses).filteredOn(s -> s == 200).hasSize(1);
        assertThat(statuses).filteredOn(s -> s == 409).hasSize(4);

        // Y en la base queda una sola jornada abierta para ese empleado.
        String today = java.time.LocalDate.now().toString();
        MvcResult res = mockMvc.perform(get("/api/analytics/employees/" + employeeId + "/entries")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("from", today).param("to", today))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode content = objectMapper.readTree(res.getResponse().getContentAsString()).get("content");
        assertThat(content).hasSize(1);
    }

    @Test
    void onlyOneOfManySimultaneousBreakStartsSucceeds() throws Exception {
        String adminToken = login("admin", "admin1234");
        String kioskToken = login("kiosk", "kiosk1234");
        long employeeId = createEmployeeWithPin(adminToken, "race-break@test.com", "2222");

        String action = "{\"employeeId\":" + employeeId + ",\"pin\":\"2222\"}";
        mockMvc.perform(post("/api/kiosk/clock-in")
                        .header("Authorization", "Bearer " + kioskToken)
                        .contentType(APPLICATION_JSON).content(action))
                .andExpect(status().isOk());

        List<Integer> statuses = fireConcurrently(
                "/api/kiosk/break/start", action, kioskToken, 5);

        assertThat(statuses).filteredOn(s -> s == 200).hasSize(1);
        assertThat(statuses).filteredOn(s -> s == 409).hasSize(4);

        String today = java.time.LocalDate.now().toString();
        MvcResult res = mockMvc.perform(get("/api/analytics/employees/" + employeeId + "/entries")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("from", today).param("to", today))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode entry = objectMapper.readTree(res.getResponse().getContentAsString())
                .get("content").get(0);
        assertThat(entry.get("breaks")).hasSize(1);
    }
}
