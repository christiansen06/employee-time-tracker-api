package com.tuusuario.employee_time_tracker.Service;

import java.time.format.DateTimeParseException;
import com.tuusuario.employee_time_tracker.Exception.NoDataFoundException;
import com.tuusuario.employee_time_tracker.Exception.ResourceNotFoundException;
import com.tuusuario.employee_time_tracker.Model.Dto.BreakSummaryDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.EmployeeResponseDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.TimeEntrySummaryDTO;
import com.tuusuario.employee_time_tracker.Model.Entity.BreakEntry;
import com.tuusuario.employee_time_tracker.Model.Entity.Employee;
import com.tuusuario.employee_time_tracker.Model.Entity.TimeEntry;
import com.tuusuario.employee_time_tracker.Model.Enums.BreakStatus;
import com.tuusuario.employee_time_tracker.Model.Enums.TimeEntryStatus;
import com.tuusuario.employee_time_tracker.Repository.BreakEntryRepository;
import com.tuusuario.employee_time_tracker.Repository.EmployeeRepository;
import com.tuusuario.employee_time_tracker.Repository.TimeEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private final TimeEntryRepository timeEntryRepository;
    private final BreakEntryRepository breakEntryRepository;
    private final EmployeeRepository employeeRepository;

    public List<TimeEntrySummaryDTO> getActiveTimeEntries() {
        List<TimeEntry> activeEntries =
                timeEntryRepository.findByStatus(TimeEntryStatus.CLOCKED_IN);

        validateNotEmpty(
                activeEntries,
                () -> new NoDataFoundException("There are no active time entries.")
        );

        return activeEntries.stream()
                .map(this::mapToTimeEntrySummaryDTO)
                .toList();
    }

    public List<BreakSummaryDTO> getActiveBreaks() {
        List<BreakEntry> activeBreaks =
                breakEntryRepository.findByBreakStatus(BreakStatus.ON_BREAK);

        validateNotEmpty(
                activeBreaks,
                () -> new NoDataFoundException("There are no active breaks.")
        );

        return activeBreaks.stream()
                .map(this::mapToBreakSummaryDTO)
                .toList();
    }

    public List<EmployeeResponseDTO> getActiveEmployees() {
        List<Employee> activeEmployees = employeeRepository.findByActiveTrue();

        validateNotEmpty(
                activeEmployees,
                () -> new NoDataFoundException("There are no active employees.")
        );

        return activeEmployees.stream()
                .map(this::mapToEmployeeDTO)
                .toList();
    }

    public List<TimeEntrySummaryDTO> getEntriesBetweenDates(
            String start,
            String end
    ) {
        LocalDateTime startDate = parseDateTime(start, "start");
        LocalDateTime endDate = parseDateTime(end, "end");

        validateDateRange(startDate, endDate);

        List<TimeEntry> entries =
                timeEntryRepository.findByClockInBetween(startDate, endDate);

        validateNotEmpty(
                entries,
                () -> new NoDataFoundException(
                        "No time entries found between "
                                + startDate
                                + " and "
                                + endDate
                                + "."
                )
        );

        return entries.stream()
                .map(this::mapToTimeEntrySummaryDTO)
                .toList();
    }

    public List<TimeEntrySummaryDTO> getEmployeeEntries(Long employeeId) {
        validateEmployeeId(employeeId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee not found with id: " + employeeId
                        )
                );

        List<TimeEntry> entries = timeEntryRepository.findByEmployeeId(employee.getId());

        validateNotEmpty(
                entries,
                () -> new NoDataFoundException(
                        "No time entries found for employee with id: " + employee.getId()
                )
        );

        return entries.stream()
                .map(this::mapToTimeEntrySummaryDTO)
                .toList();
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

    private <T> void validateNotEmpty(
            Collection<T> data,
            Supplier<? extends RuntimeException> exceptionSupplier
    ) {
        if (data == null || data.isEmpty()) {
            throw exceptionSupplier.get();
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
