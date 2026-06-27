package com.tuusuario.employee_time_tracker.Service;

import com.tuusuario.employee_time_tracker.Exception.ResourceNotFoundException;
import com.tuusuario.employee_time_tracker.Model.Dto.BreakEndRequestDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.BreakResponseDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.BreakStartRequestDTO;
import com.tuusuario.employee_time_tracker.Model.Entity.BreakEntry;
import com.tuusuario.employee_time_tracker.Model.Entity.Employee;
import com.tuusuario.employee_time_tracker.Model.Entity.TimeEntry;
import com.tuusuario.employee_time_tracker.Model.Enums.BreakStatus;
import com.tuusuario.employee_time_tracker.Model.Enums.TimeEntryStatus;
import com.tuusuario.employee_time_tracker.Repository.BreakEntryRepository;
import com.tuusuario.employee_time_tracker.Repository.TimeEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class BreakService {

    private final BreakEntryRepository breakEntryRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final CurrentEmployeeService currentEmployeeService;

    /** Jornada "abierta": fichada y aun no finalizada (incluye estar en break). */
    private static final List<TimeEntryStatus> OPEN_STATUSES =
            List.of(TimeEntryStatus.CLOCKED_IN, TimeEntryStatus.ON_BREAK);

    // ---------- ADMIN: operando por id ----------

    public BreakResponseDTO startBreak(BreakStartRequestDTO dto) {
        TimeEntry timeEntry = timeEntryRepository.findById(dto.getTimeEntryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Time entry not found with id: " + dto.getTimeEntryId()));
        return doStartBreak(timeEntry);
    }

    public BreakResponseDTO endBreak(BreakEndRequestDTO dto) {
        BreakEntry breakEntry = breakEntryRepository.findById(dto.getBreakEntryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Break entry not found with id: " + dto.getBreakEntryId()));
        return doEndBreak(breakEntry);
    }

    // ---------- SELF (/me): operando sobre el usuario autenticado ----------

    public BreakResponseDTO startBreakCurrent(String username) {
        return startBreakByEmployeeId(currentEmployeeService.getByUsername(username).getId());
    }

    public BreakResponseDTO endBreakCurrent(String username) {
        return endBreakByEmployeeId(currentEmployeeService.getByUsername(username).getId());
    }

    // ---------- KIOSCO: operando por employeeId ----------

    public BreakResponseDTO startBreakByEmployeeId(Long employeeId) {
        TimeEntry timeEntry = timeEntryRepository
                .findFirstByEmployeeIdAndStatusInOrderByClockInDesc(
                        employeeId, OPEN_STATUSES)
                .orElseThrow(() -> new IllegalStateException(
                        "There is no open time entry. Clock in before starting a break."));

        return doStartBreak(timeEntry);
    }

    public BreakResponseDTO endBreakByEmployeeId(Long employeeId) {
        BreakEntry breakEntry = breakEntryRepository
                .findFirstByTimeEntry_Employee_IdAndBreakStatus(
                        employeeId, BreakStatus.ON_BREAK)
                .orElseThrow(() -> new IllegalStateException(
                        "There is no active break to end."));

        return doEndBreak(breakEntry);
    }

    public List<BreakResponseDTO> getActiveBreaks() {
        return breakEntryRepository.findByBreakStatus(BreakStatus.ON_BREAK)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    // ---------- Logica + validaciones de negocio ----------

    private BreakResponseDTO doStartBreak(TimeEntry timeEntry) {

        // No puede haber dos breaks activos en la misma jornada.
        boolean hasActiveBreak = timeEntry.getBreaks() != null
                && timeEntry.getBreaks().stream()
                .anyMatch(b -> b.getBreakStatus() == BreakStatus.ON_BREAK);

        if (hasActiveBreak) {
            throw new IllegalStateException(
                    "There is already an active break for this time entry.");
        }

        // El empleado tiene que estar fichado (no finalizado, no ya en break).
        if (timeEntry.getStatus() != TimeEntryStatus.CLOCKED_IN) {
            throw new IllegalStateException(
                    "Cannot start break because employee is not clocked in.");
        }

        BreakEntry breakEntry = BreakEntry.builder()
                .breakStart(LocalDateTime.now())
                .breakStatus(BreakStatus.ON_BREAK)
                .timeEntry(timeEntry)
                .build();

        timeEntry.setStatus(TimeEntryStatus.ON_BREAK);
        timeEntryRepository.save(timeEntry);

        return mapToDTO(breakEntryRepository.save(breakEntry));
    }

    private BreakResponseDTO doEndBreak(BreakEntry breakEntry) {

        if (breakEntry.getBreakStatus() != BreakStatus.ON_BREAK) {
            throw new IllegalStateException("This break has already been finished.");
        }

        LocalDateTime endTime = LocalDateTime.now();
        long duration = Duration.between(breakEntry.getBreakStart(), endTime).toMinutes();

        breakEntry.setBreakEnd(endTime);
        breakEntry.setDurationMinutes(duration);
        breakEntry.setBreakStatus(BreakStatus.FINISHED);

        TimeEntry timeEntry = breakEntry.getTimeEntry();
        if (timeEntry != null) {
            timeEntry.setStatus(TimeEntryStatus.CLOCKED_IN);
            timeEntryRepository.save(timeEntry);
        }

        return mapToDTO(breakEntryRepository.save(breakEntry));
    }

    private BreakResponseDTO mapToDTO(BreakEntry breakEntry) {
        return BreakResponseDTO.builder()
                .id(breakEntry.getId())
                .breakStart(breakEntry.getBreakStart())
                .breakEnd(breakEntry.getBreakEnd())
                .durationMinutes(breakEntry.getDurationMinutes())
                .breakStatus(breakEntry.getBreakStatus())
                .build();
    }
}
