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
    private final AuditLogService auditLogService;
    private final PaidPeriodGuard paidPeriodGuard;

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

    // ---------- ADMIN: correccion manual de breaks ----------

    /**
     * Agrega un break que el empleado nunca registro (se lo tomo pero no
     * lo ficho). Nace ya cerrado.
     */
    public BreakResponseDTO createManualBreak(Long timeEntryId,
                                              LocalDateTime start,
                                              LocalDateTime end) {

        TimeEntry timeEntry = timeEntryRepository.findById(timeEntryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Time entry not found with id: " + timeEntryId));

        assertEditable(timeEntry);
        validateBreakWindow(timeEntry, start, end, null);

        BreakEntry saved = breakEntryRepository.save(BreakEntry.builder()
                .breakStart(start)
                .breakEnd(end)
                .durationMinutes(Duration.between(start, end).toMinutes())
                .breakStatus(BreakStatus.FINISHED)
                .timeEntry(timeEntry)
                .build());

        auditLogService.record("BREAK", saved.getId(), "CREATE",
                "manual: timeEntryId=" + timeEntryId
                        + ", " + start + " -> " + end);

        return mapToDTO(saved);
    }

    /**
     * Corrige el horario de un break. Caso tipico: el empleado volvio a
     * trabajar y se olvido de cerrarlo, asi que el break quedo abierto o
     * con una duracion que no es real.
     */
    public BreakResponseDTO updateBreak(Long breakId,
                                        LocalDateTime start,
                                        LocalDateTime end) {

        BreakEntry breakEntry = breakEntryRepository.findById(breakId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Break entry not found with id: " + breakId));

        TimeEntry timeEntry = breakEntry.getTimeEntry();
        assertEditable(timeEntry);
        validateBreakWindow(timeEntry, start, end, breakId);

        auditLogService.record("BREAK", breakId, "UPDATE",
                "before: " + breakEntry.getBreakStart() + " -> " + breakEntry.getBreakEnd()
                        + " | after: " + start + " -> " + end);

        breakEntry.setBreakStart(start);
        breakEntry.setBreakEnd(end);
        breakEntry.setDurationMinutes(Duration.between(start, end).toMinutes());
        breakEntry.setBreakStatus(BreakStatus.FINISHED);

        BreakResponseDTO dto = mapToDTO(breakEntryRepository.save(breakEntry));

        reopenIfNoActiveBreak(timeEntry);

        return dto;
    }

    /** Borra un break cargado por error. */
    public void deleteBreak(Long breakId) {

        BreakEntry breakEntry = breakEntryRepository.findById(breakId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Break entry not found with id: " + breakId));

        TimeEntry timeEntry = breakEntry.getTimeEntry();
        assertEditable(timeEntry);

        auditLogService.record("BREAK", breakId, "DELETE",
                "timeEntryId=" + (timeEntry != null ? timeEntry.getId() : null)
                        + ", " + breakEntry.getBreakStart()
                        + " -> " + breakEntry.getBreakEnd());

        breakEntryRepository.delete(breakEntry);

        reopenIfNoActiveBreak(timeEntry);
    }

    // ---------- Validaciones de la correccion manual ----------

    /** Un break de un periodo ya liquidado no se toca (cambiaria lo pagado). */
    private void assertEditable(TimeEntry timeEntry) {
        if (timeEntry != null && timeEntry.getEmployee() != null
                && timeEntry.getClockIn() != null) {
            paidPeriodGuard.assertNotPaid(timeEntry.getEmployee().getId(),
                    timeEntry.getClockIn().toLocalDate());
        }
    }

    /**
     * El break tiene que cerrar: fin posterior al inicio, contenido en la
     * jornada y sin pisarse con otro break del mismo dia.
     */
    private void validateBreakWindow(TimeEntry timeEntry,
                                     LocalDateTime start,
                                     LocalDateTime end,
                                     Long excludeBreakId) {

        if (start == null || end == null) {
            throw new IllegalArgumentException("breakStart and breakEnd are required.");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("breakEnd must be after breakStart.");
        }
        if (timeEntry == null) {
            return;
        }
        if (timeEntry.getClockIn() != null && start.isBefore(timeEntry.getClockIn())) {
            throw new IllegalStateException(
                    "The break cannot start before the shift starts.");
        }
        if (timeEntry.getClockOut() != null && end.isAfter(timeEntry.getClockOut())) {
            throw new IllegalStateException(
                    "The break cannot end after the shift ends.");
        }

        boolean overlaps = timeEntry.getBreaks() != null
                && timeEntry.getBreaks().stream()
                .filter(b -> excludeBreakId == null || !excludeBreakId.equals(b.getId()))
                .filter(b -> b.getBreakStart() != null && b.getBreakEnd() != null)
                .anyMatch(b -> start.isBefore(b.getBreakEnd())
                        && end.isAfter(b.getBreakStart()));

        if (overlaps) {
            throw new IllegalStateException(
                    "This break overlaps another break of the same shift.");
        }
    }

    /**
     * Si ya no queda ningun break abierto, la jornada vuelve a estado de
     * trabajo: asi el empleado puede volver a fichar salida con normalidad.
     */
    private void reopenIfNoActiveBreak(TimeEntry timeEntry) {
        if (timeEntry == null
                || timeEntry.getStatus() != TimeEntryStatus.ON_BREAK
                || timeEntry.getEmployee() == null) {
            return;
        }

        boolean stillOnBreak = breakEntryRepository
                .findFirstByTimeEntry_Employee_IdAndBreakStatus(
                        timeEntry.getEmployee().getId(), BreakStatus.ON_BREAK)
                .isPresent();

        if (!stillOnBreak) {
            timeEntry.setStatus(TimeEntryStatus.CLOCKED_IN);
            timeEntryRepository.save(timeEntry);
        }
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
