package com.tuusuario.employee_time_tracker.Controller;

import com.tuusuario.employee_time_tracker.Model.Dto.BreakSummaryDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.EmployeeResponseDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.TimeEntrySummaryDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.WeeklyHoursDetailDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.AbsenceDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.AnalyticsSummaryDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.OvertimeDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.PunctualityDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.TrendPointDTO;
import com.tuusuario.employee_time_tracker.Model.Entity.AuditLog;
import com.tuusuario.employee_time_tracker.Service.AnalyticsService;
import com.tuusuario.employee_time_tracker.Service.AuditLogService;
import com.tuusuario.employee_time_tracker.Service.BusinessMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
    private final AnalyticsService analyticsService;
    private final AuditLogService auditLogService;
    private final BusinessMetricsService businessMetricsService;

    // ---------- Metricas de negocio (sin rango: ultimos 30 dias) ----------

    /** Resumen ejecutivo: horas, breaks, headcount y costo laboral estimado. */
    @GetMapping("/summary")
    public AnalyticsSummaryDTO getSummary(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return businessMetricsService.getSummary(from, to);
    }

    @GetMapping("/summary/csv")
    public ResponseEntity<byte[]> getSummaryCsv(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return csvResponse(businessMetricsService.buildSummaryCsv(from, to),
                "resumen-horas.csv");
    }

    /** Llegadas tarde vs hora esperada de entrada (empleados con hora cargada). */
    @GetMapping("/punctuality")
    public List<PunctualityDTO> getPunctuality(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return businessMetricsService.getPunctuality(from, to);
    }

    /** Horas extra por exceso diario y por exceso del tope semanal. */
    @GetMapping("/overtime")
    public List<OvertimeDTO> getOvertime(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return businessMetricsService.getOvertime(from, to);
    }

    @GetMapping("/overtime/csv")
    public ResponseEntity<byte[]> getOvertimeCsv(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return csvResponse(businessMetricsService.buildOvertimeCsv(from, to),
                "horas-extra.csv");
    }

    /** Dias operativos del local en los que cada empleado no ficho. */
    @GetMapping("/absences")
    public List<AbsenceDTO> getAbsences(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return businessMetricsService.getAbsences(from, to);
    }

    /** Serie diaria de horas trabajadas y gente que trabajo (para graficos). */
    @GetMapping("/trends")
    public List<TrendPointDTO> getTrends(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return businessMetricsService.getTrends(from, to);
    }

    private ResponseEntity<byte[]> csvResponse(String csv, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    /** Bitacora de cambios manuales (ediciones/borrados de jornadas), paginada. */
    @GetMapping("/audit-log")
    public Page<AuditLog> getAuditLog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return auditLogService.getPage(page, size);
    }

    @GetMapping("/active-time-entries")
    public List<TimeEntrySummaryDTO> getActiveTimeEntries() {
        return analyticsService.getActiveTimeEntries();
    }

    @GetMapping("/active-breaks")
    public List<BreakSummaryDTO> getActiveBreaks() {
        return analyticsService.getActiveBreaks();
    }

    @GetMapping("/active-employees")
    public List<EmployeeResponseDTO> getActiveEmployees() {
        return analyticsService.getActiveEmployees();
    }

    @GetMapping("/entries")
    public Page<TimeEntrySummaryDTO> getEntriesBetweenDates(
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) {
        return analyticsService.getEntriesBetweenDates(start, end, buildPageable(page, size));
    }

    @GetMapping("/employees/{employeeId}/entries")
    public Page<TimeEntrySummaryDTO> getEmployeeEntries(
            @PathVariable Long employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) {
        return analyticsService.getEmployeeEntries(employeeId, buildPageable(page, size));
    }

    /** Paginado defensivo: nunca mas de 500 filas por pagina, siempre ordenado. */
    private Pageable buildPageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 500),
                Sort.by(Sort.Direction.DESC, "clockIn"));
    }

    /**
     * Cuadro semanal (todos los empleados activos) de la semana que
     * contiene la fecha indicada; sin fecha usa la semana actual.
     */
    @GetMapping("/weekly-report")
    public List<WeeklyHoursDetailDTO> getWeeklyReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return analyticsService.getWeeklyReport(date);
    }

    /** El mismo cuadro semanal, descargable como CSV para liquidacion. */
    @GetMapping("/weekly-report/csv")
    public ResponseEntity<byte[]> getWeeklyReportCsv(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate anchor = date != null ? date : LocalDate.now();
        LocalDate monday = anchor.with(DayOfWeek.MONDAY);

        String csv = analyticsService.buildWeeklyCsv(anchor);
        String filename = "horas-semana-" + monday + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }
}
