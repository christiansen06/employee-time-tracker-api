package com.tuusuario.employee_time_tracker.Controller;

import com.tuusuario.employee_time_tracker.Model.Dto.BreakSummaryDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.EmployeeResponseDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.TimeEntrySummaryDTO;
import com.tuusuario.employee_time_tracker.Service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
