package com.tuusuario.employee_time_tracker.Controller;

import com.tuusuario.employee_time_tracker.Model.Dto.BreakSummaryDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.EmployeeResponseDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.TimeEntrySummaryDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.WeeklyHoursDetailDTO;
import com.tuusuario.employee_time_tracker.Service.AnalyticsService;
import lombok.RequiredArgsConstructor;
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
    public List<TimeEntrySummaryDTO> getEntriesBetweenDates(
            @RequestParam String start,
            @RequestParam String end
    ) {
        return analyticsService.getEntriesBetweenDates(start, end);
    }

    @GetMapping("/employees/{employeeId}/entries")
    public List<TimeEntrySummaryDTO> getEmployeeEntries(
            @PathVariable Long employeeId
    ) {
        return analyticsService.getEmployeeEntries(employeeId);
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
