package com.tuusuario.employee_time_tracker.Service;

import java.time.format.DateTimeParseException;
import com.tuusuario.employee_time_tracker.Exception.ResourceNotFoundException;
import com.tuusuario.employee_time_tracker.Model.Dto.BreakSummaryDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.DailyHoursDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.EmployeeResponseDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.TimeEntrySummaryDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.WeeklyHoursDetailDTO;
import com.tuusuario.employee_time_tracker.Model.Entity.BreakEntry;
import com.tuusuario.employee_time_tracker.Model.Entity.Employee;
import com.tuusuario.employee_time_tracker.Model.Entity.TimeEntry;
import com.tuusuario.employee_time_tracker.Model.Enums.BreakStatus;
import com.tuusuario.employee_time_tracker.Model.Enums.TimeEntryStatus;
import com.tuusuario.employee_time_tracker.Repository.BreakEntryRepository;
import com.tuusuario.employee_time_tracker.Repository.EmployeeRepository;
import com.tuusuario.employee_time_tracker.Repository.TimeEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private final TimeEntryRepository timeEntryRepository;
    private final BreakEntryRepository breakEntryRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeService employeeService;

    // Nota de diseño: las consultas sin resultados devuelven listas vacias
    // (200), no 404. Un rango sin fichadas es un caso normal, no un error.

    public List<TimeEntrySummaryDTO> getActiveTimeEntries() {
        // Incluye tambien a los que estan en break: su jornada sigue abierta.
        return timeEntryRepository.findByStatusIn(
                        List.of(TimeEntryStatus.CLOCKED_IN, TimeEntryStatus.ON_BREAK))
                .stream()
                .map(this::mapToTimeEntrySummaryDTO)
                .toList();
    }

    public List<BreakSummaryDTO> getActiveBreaks() {
        return breakEntryRepository.findByBreakStatus(BreakStatus.ON_BREAK)
                .stream()
                .map(this::mapToBreakSummaryDTO)
                .toList();
    }

    public List<EmployeeResponseDTO> getActiveEmployees() {
        return employeeRepository.findByActiveTrue()
                .stream()
                .map(this::mapToEmployeeDTO)
                .toList();
    }

    public Page<TimeEntrySummaryDTO> getEntriesBetweenDates(
            String start,
            String end,
            Pageable pageable
    ) {
        LocalDateTime startDate = parseDateTime(start, "start");
        LocalDateTime endDate = parseDateTime(end, "end");

        validateDateRange(startDate, endDate);

        return timeEntryRepository
                .findByClockInBetween(startDate, endDate, pageable)
                .map(this::mapToTimeEntrySummaryDTO);
    }

    public Page<TimeEntrySummaryDTO> getEmployeeEntries(Long employeeId, Pageable pageable) {
        validateEmployeeId(employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee not found with id: " + employeeId
                        )
                );

        return timeEntryRepository
                .findByEmployeeId(employee.getId(), pageable)
                .map(this::mapToTimeEntrySummaryDTO);
    }

    /**
     * Cuadro semanal de TODOS los empleados activos para la semana
     * que contiene la fecha ancla (o la actual si es null). Para liquidacion.
     */
    public List<WeeklyHoursDetailDTO> getWeeklyReport(LocalDate anchor) {
        LocalDate effective = anchor != null ? anchor : LocalDate.now();

        return employeeRepository.findByActiveTrue().stream()
                .map(e -> employeeService.getWeeklyDetail(e.getId(), effective))
                .toList();
    }

    /** El mismo cuadro semanal en CSV (compatible con Excel es-AR: separador ';'). */
    public String buildWeeklyCsv(LocalDate anchor) {
        List<WeeklyHoursDetailDTO> report = getWeeklyReport(anchor);
        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM");
        String[] dayNames = {"Lun", "Mar", "Mie", "Jue", "Vie", "Sab", "Dom"};

        // BOM para que Excel abra el archivo como UTF-8.
        StringBuilder sb = new StringBuilder("﻿");

        sb.append("Empleado");
        if (!report.isEmpty()) {
            List<DailyHoursDTO> days = report.get(0).getDays();
            for (int i = 0; i < days.size(); i++) {
                sb.append(";").append(dayNames[i]).append(" ").append(days.get(i).getDate().format(df));
            }
        }
        sb.append(";Total\n");

        for (WeeklyHoursDetailDTO row : report) {
            sb.append(row.getEmployeeName());
            for (DailyHoursDTO day : row.getDays()) {
                sb.append(";").append(formatMinutes(day.getWorkedMinutes()));
            }
            sb.append(";").append(formatMinutes(row.getTotalWorkedMinutes())).append("\n");
        }

        return sb.toString();
    }

    private String formatMinutes(long minutes) {
        return minutes / 60 + ":" + String.format("%02d", minutes % 60);
    }

    private void validateEmployeeId(Long employeeId) {
        if (employeeId == null || employeeId <= 0) {
            throw new IllegalArgumentException("employeeId must be greater than 0.");
        }
    }

    private LocalDateTime parseDateTime(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName +
                    " parameter is required.");
        }

        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(
                    "Invalid " + fieldName +
                            " date format. Use ISO-8601, for example: 2026-05-25T10:00:00."
            );
        }
    }

    private void validateDateRange(LocalDateTime start, LocalDateTime end) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("start date cannot be after end date.");
        }
    }

    private TimeEntrySummaryDTO mapToTimeEntrySummaryDTO(TimeEntry timeEntry) {
        Employee employee = timeEntry.getEmployee();

        return TimeEntrySummaryDTO.builder()
                .timeEntryId(timeEntry.getId())
                .employeeId(employee.getId())
                .employeeName(employee.getName() + " " + employee.getLastName())
                .clockIn(timeEntry.getClockIn())
                .clockOut(timeEntry.getClockOut())
                .status(timeEntry.getStatus())
                .build();
    }

    private BreakSummaryDTO mapToBreakSummaryDTO(BreakEntry breakEntry) {
        Employee employee = breakEntry.getTimeEntry().getEmployee();

        return BreakSummaryDTO.builder()
                .breakId(breakEntry.getId())
                .employeeId(employee.getId())
                .employeeName(employee.getName() + " " + employee.getLastName())
                .breakStart(breakEntry.getBreakStart())
                .build();
    }

    private EmployeeResponseDTO mapToEmployeeDTO(Employee employee) {
        return EmployeeResponseDTO.builder()
                .id(employee.getId())
                .name(employee.getName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .position(employee.getPosition())
                .active(employee.getActive())
                .build();
    }
}
