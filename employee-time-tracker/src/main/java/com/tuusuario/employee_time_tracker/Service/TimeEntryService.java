package com.tuusuario.employee_time_tracker.Service;

import com.tuusuario.employee_time_tracker.Exception.ResourceNotFoundException;
import com.tuusuario.employee_time_tracker.Model.Dto.CurrentStatusDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.TimeEntrySummaryDTO;
import com.tuusuario.employee_time_tracker.Model.Entity.BreakEntry;
import com.tuusuario.employee_time_tracker.Model.Entity.Employee;
import com.tuusuario.employee_time_tracker.Model.Entity.TimeEntry;
import com.tuusuario.employee_time_tracker.Model.Enums.BreakStatus;
import com.tuusuario.employee_time_tracker.Model.Enums.TimeEntryStatus;
import com.tuusuario.employee_time_tracker.Model.Enums.WorkState;
import com.tuusuario.employee_time_tracker.Repository.BreakEntryRepository;
import com.tuusuario.employee_time_tracker.Repository.EmployeeRepository;
import com.tuusuario.employee_time_tracker.Repository.TimeEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class TimeEntryService {

    private final TimeEntryRepository timeEntryRepository;
    private final EmployeeRepository employeeRepository;
    private final BreakEntryRepository breakEntryRepository;
    private final CurrentEmployeeService currentEmployeeService;
    private final AuditLogService auditLogService;

    /** Jornada "abierta": fichada y aun no finalizada (incluye estar en break). */
    private static final List<TimeEntryStatus> OPEN_STATUSES =
            List.of(TimeEntryStatus.CLOCKED_IN, TimeEntryStatus.ON_BREAK);

    // ---------- ADMIN: operando por id ----------

    public TimeEntrySummaryDTO clockIn(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with id: " + employeeId));
        return doClockIn(employee);
    }

    public TimeEntrySummaryDTO clockOut(Long timeEntryId) {
        TimeEntry timeEntry = timeEntryRepository.findById(timeEntryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Time entry not found with id: " + timeEntryId));
        return doClockOut(timeEntry);
    }

    // ---------- SELF (/me): operando sobre el usuario autenticado ----------

    public TimeEntrySummaryDTO clockInCurrent(String username) {
        return doClockIn(currentEmployeeService.getByUsername(username));
    }

    public TimeEntrySummaryDTO clockOutCurrent(String username) {
        return clockOutForEmployee(currentEmployeeService.getByUsername(username));
    }

    public CurrentStatusDTO getCurrentStatus(String username) {
        return buildStatus(currentEmployeeService.getByUsername(username));
    }

    // ---------- KIOSCO: operando por employeeId ----------

    public TimeEntrySummaryDTO clockOutByEmployeeId(Long employeeId) {
        return clockOutForEmployee(loadEmployee(employeeId));
    }

    public CurrentStatusDTO getStatusByEmployeeId(Long employeeId) {
        return buildStatus(loadEmployee(employeeId));
    }

    public WorkState getWorkState(Long employeeId) {
        return buildStatus(loadEmployee(employeeId)).getState();
    }

    // ---------- ADMIN: alta manual de jornadas ----------

    /**
     * Crea una jornada ya cerrada para un empleado (ej.: trabajo pero se
     * olvido de fichar). Queda registrada en la bitacora de auditoria.
     */
    public TimeEntrySummaryDTO createManualEntry(Long employeeId,
                                                 LocalDateTime clockIn,
                                                 LocalDateTime clockOut) {

        Employee employee = loadEmployee(employeeId);

        if (!clockOut.isAfter(clockIn)) {
            throw new IllegalArgumentException("clockOut must be after clockIn.");
        }

        // No permitir superposicion con otra jornada ya registrada:
        // duplicaria horas en reportes y liquidacion.
        if (timeEntryRepository.existsByEmployeeIdAndClockInLessThanAndClockOutGreaterThan(
                employeeId, clockOut, clockIn)) {
            throw new IllegalStateException(
                    "This time range overlaps an existing time entry for the employee.");
        }

        TimeEntry entry = TimeEntry.builder()
                .clockIn(clockIn)
                .clockOut(clockOut)
                .status(TimeEntryStatus.FINISHED)
                .autoClosed(false)
                .employee(employee)
                .build();

        TimeEntry saved = timeEntryRepository.save(entry);

        auditLogService.record("TIME_ENTRY", saved.getId(), "CREATE",
                "manual: employeeId=" + employeeId
                        + ", clockIn=" + clockIn + ", clockOut=" + clockOut);

        return mapToDTO(saved);
    }

    // ---------- ADMIN: correccion manual de jornadas ----------

    public TimeEntrySummaryDTO updateEntry(Long timeEntryId,
                                           LocalDateTime clockIn,
                                           LocalDateTime clockOut) {

        TimeEntry timeEntry = timeEntryRepository.findById(timeEntryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Time entry not found with id: " + timeEntryId));

        if (!clockOut.isAfter(clockIn)) {
            throw new IllegalArgumentException(
                    "clockOut must be after clockIn.");
        }

        auditLogService.record("TIME_ENTRY", timeEntry.getId(), "UPDATE",
                "before: clockIn=" + timeEntry.getClockIn()
                        + ", clockOut=" + timeEntry.getClockOut()
                        + " | after: clockIn=" + clockIn + ", clockOut=" + clockOut);

        // Una jornada corregida queda siempre cerrada y deja de estar
        // marcada como auto-cerrada (el admin ya la reviso).
        timeEntry.setClockIn(clockIn);
        timeEntry.setClockOut(clockOut);
        timeEntry.setStatus(TimeEntryStatus.FINISHED);
        timeEntry.setAutoClosed(false);

        return mapToDTO(timeEntryRepository.save(timeEntry));
    }

    /**
     * Marca o desmarca una jornada como pagada al doble (feriado).
     * Afecta la liquidacion: sus minutos cuentan x2. Queda auditado.
     */
    public TimeEntrySummaryDTO setPaidDouble(Long timeEntryId, boolean paidDouble) {

        TimeEntry timeEntry = timeEntryRepository.findById(timeEntryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Time entry not found with id: " + timeEntryId));

        boolean previous = Boolean.TRUE.equals(timeEntry.getPaidDouble());

        if (previous != paidDouble) {
            auditLogService.record("TIME_ENTRY", timeEntryId, "UPDATE",
                    "paidDouble: " + previous + " -> " + paidDouble);
            timeEntry.setPaidDouble(paidDouble);
            timeEntry = timeEntryRepository.save(timeEntry);
        }

        return mapToDTO(timeEntry);
    }

    /** Elimina una jornada (y sus breaks). Para borrar dias de prueba o errores. */
    public void deleteEntry(Long timeEntryId) {

        TimeEntry timeEntry = timeEntryRepository.findById(timeEntryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Time entry not found with id: " + timeEntryId));

        auditLogService.record("TIME_ENTRY", timeEntry.getId(), "DELETE",
                "employeeId=" + (timeEntry.getEmployee() != null
                        ? timeEntry.getEmployee().getId() : null)
                        + ", clockIn=" + timeEntry.getClockIn()
                        + ", clockOut=" + timeEntry.getClockOut());

        if (timeEntry.getBreaks() != null && !timeEntry.getBreaks().isEmpty()) {
            breakEntryRepository.deleteAll(timeEntry.getBreaks());
        }

        timeEntryRepository.delete(timeEntry);
    }

    // ---------- Estado actual (para el frontend) ----------

    private TimeEntrySummaryDTO clockOutForEmployee(Employee employee) {
        TimeEntry open = timeEntryRepository
                .findFirstByEmployeeIdAndStatusInOrderByClockInDesc(
                        employee.getId(), OPEN_STATUSES)
                .orElseThrow(() -> new IllegalStateException(
                        "There is no open time entry to clock out."));

        return doClockOut(open);
    }

    private Employee loadEmployee(Long employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with id: " + employeeId));
    }

    private CurrentStatusDTO buildStatus(Employee employee) {

        CurrentStatusDTO.CurrentStatusDTOBuilder status = CurrentStatusDTO.builder()
                .employeeId(employee.getId())
                .employeeName(employee.getName() + " " + employee.getLastName());

        TimeEntry open = timeEntryRepository
                .findFirstByEmployeeIdAndStatusInOrderByClockInDesc(
                        employee.getId(), OPEN_STATUSES)
                .orElse(null);

        if (open == null) {
            return status.state(WorkState.NO_SHIFT).build();
        }

        status.timeEntryId(open.getId()).clockIn(open.getClockIn());

        if (open.getStatus() == TimeEntryStatus.ON_BREAK) {
            breakEntryRepository
                    .findFirstByTimeEntry_Employee_IdAndBreakStatus(
                            employee.getId(), BreakStatus.ON_BREAK)
                    .ifPresent(b -> status.activeBreakId(b.getId())
                            .breakStart(b.getBreakStart()));
            return status.state(WorkState.ON_BREAK).build();
        }

        return status.state(WorkState.CLOCKED_IN).build();
    }

    // ---------- Logica + validaciones de negocio ----------

    private TimeEntrySummaryDTO doClockIn(Employee employee) {

        // No permitir dos jornadas abiertas a la vez.
        if (timeEntryRepository.existsByEmployeeIdAndStatusIn(
                employee.getId(), OPEN_STATUSES)) {
            throw new IllegalStateException(
                    "Employee already has an open time entry. "
                            + "Clock out before clocking in again.");
        }

        TimeEntry timeEntry = TimeEntry.builder()
                .clockIn(LocalDateTime.now())
                .status(TimeEntryStatus.CLOCKED_IN)
                .employee(employee)
                .build();

        return mapToDTO(timeEntryRepository.save(timeEntry));
    }

    private TimeEntrySummaryDTO doClockOut(TimeEntry timeEntry) {

        if (timeEntry.getStatus() == TimeEntryStatus.FINISHED) {
            throw new IllegalStateException("This time entry is already finished.");
        }

        if (timeEntry.getStatus() == TimeEntryStatus.ON_BREAK) {
            throw new IllegalStateException(
                    "Cannot clock out while on break. End the break first.");
        }

        timeEntry.setClockOut(LocalDateTime.now());
        timeEntry.setStatus(TimeEntryStatus.FINISHED);

        return mapToDTO(timeEntryRepository.save(timeEntry));
    }

    private TimeEntrySummaryDTO mapToDTO(TimeEntry timeEntry) {
        Employee employee = timeEntry.getEmployee();

        return TimeEntrySummaryDTO.builder()
                .timeEntryId(timeEntry.getId())
                .employeeId(employee.getId())
                .employeeName(employee.getName() + " " + employee.getLastName())
                .clockIn(timeEntry.getClockIn())
                .clockOut(timeEntry.getClockOut())
                .status(timeEntry.getStatus())
                .autoClosed(timeEntry.getAutoClosed())
                .paidDouble(timeEntry.getPaidDouble())
                .build();
    }
}
