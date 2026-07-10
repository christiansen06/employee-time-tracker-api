package com.tuusuario.employee_time_tracker.Controller;

import com.tuusuario.employee_time_tracker.Model.Dto.CreateTimeEntryRequestDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.CurrentStatusDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.TimeEntrySummaryDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.UpdateTimeEntryRequestDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.WorkedHoursResponseDTO;
import com.tuusuario.employee_time_tracker.Service.EmployeeService;
import com.tuusuario.employee_time_tracker.Service.TimeEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/time-entries")
@RequiredArgsConstructor
public class TimeEntryController {

    private final TimeEntryService timeEntryService;
    private final EmployeeService employeeService;

    // ---------- ADMIN: alta manual de una jornada olvidada ----------

    /** Crea una jornada ya cerrada (el empleado trabajo pero no ficho). */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TimeEntrySummaryDTO> createEntry(
            @Valid @RequestBody CreateTimeEntryRequestDTO dto) {
        return ResponseEntity.ok(timeEntryService.createManualEntry(
                dto.getEmployeeId(), dto.getClockIn(), dto.getClockOut()));
    }

    // ---------- ADMIN: fichar a cualquier empleado por id ----------

    @PostMapping("/clock-in/{employeeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TimeEntrySummaryDTO> clockIn(@PathVariable Long employeeId) {
        return ResponseEntity.ok(timeEntryService.clockIn(employeeId));
    }

    @PostMapping("/clock-out/{timeEntryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TimeEntrySummaryDTO> clockOut(@PathVariable Long timeEntryId) {
        return ResponseEntity.ok(timeEntryService.clockOut(timeEntryId));
    }

    /** Eliminacion de una jornada (dias de prueba o registros erroneos). */
    @DeleteMapping("/{timeEntryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteEntry(@PathVariable Long timeEntryId) {
        timeEntryService.deleteEntry(timeEntryId);
        return ResponseEntity.noContent().build();
    }

    /** Correccion manual de una jornada (horario de entrada y salida). */
    @PutMapping("/{timeEntryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TimeEntrySummaryDTO> updateEntry(
            @PathVariable Long timeEntryId,
            @Valid @RequestBody UpdateTimeEntryRequestDTO dto) {
        return ResponseEntity.ok(
                timeEntryService.updateEntry(timeEntryId, dto.getClockIn(), dto.getClockOut()));
    }

    // ---------- SELF: estado actual del empleado autenticado ----------

    /** Estado actual (sin jornada / fichado / en break) para habilitar botones. */
    @GetMapping("/me/current")
    public ResponseEntity<CurrentStatusDTO> myCurrentStatus(Authentication authentication) {
        return ResponseEntity.ok(timeEntryService.getCurrentStatus(authentication.getName()));
    }

    // ---------- SELF: el empleado autenticado ficha su propia jornada ----------

    @PostMapping("/me/clock-in")
    public ResponseEntity<TimeEntrySummaryDTO> clockInMe(Authentication authentication) {
        return ResponseEntity.ok(timeEntryService.clockInCurrent(authentication.getName()));
    }

    @PostMapping("/me/clock-out")
    public ResponseEntity<TimeEntrySummaryDTO> clockOutMe(Authentication authentication) {
        return ResponseEntity.ok(timeEntryService.clockOutCurrent(authentication.getName()));
    }

    // ---------- SELF: horas trabajadas del empleado autenticado ----------

    /** Botón "mis horas de esta semana" (netas de breaks). */
    @GetMapping("/me/worked-hours/week")
    public ResponseEntity<WorkedHoursResponseDTO> myWeeklyWorkedHours(Authentication authentication) {
        return ResponseEntity.ok(
                employeeService.getWeeklyWorkedHoursForCurrentUser(authentication.getName()));
    }

    /** Horas trabajadas históricas totales del empleado autenticado. */
    @GetMapping("/me/worked-hours")
    public ResponseEntity<WorkedHoursResponseDTO> myWorkedHours(Authentication authentication) {
        return ResponseEntity.ok(
                employeeService.getWorkedHoursForCurrentUser(authentication.getName()));
    }
}
