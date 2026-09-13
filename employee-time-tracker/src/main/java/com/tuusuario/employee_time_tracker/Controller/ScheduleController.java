package com.tuusuario.employee_time_tracker.Controller;

import com.tuusuario.employee_time_tracker.Model.Dto.CopyWeekRequestDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.ScheduleCellDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.ScheduleWeekDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.UpsertScheduleRequestDTO;
import com.tuusuario.employee_time_tracker.Service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** Armado del horario semanal (solo ADMIN). */
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping
    public ScheduleWeekDTO getWeek(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return scheduleService.getWeek(from, to);
    }

    @PutMapping
    public ScheduleCellDTO upsert(@Valid @RequestBody UpsertScheduleRequestDTO dto) {
        return scheduleService.upsertCell(dto.getEmployeeId(), dto.getDate(),
                dto.getStartTime(), dto.getEndTime(), dto.isDayOff(), dto.getNote());
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(
            @RequestParam Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        scheduleService.deleteCell(employeeId, date);
        return ResponseEntity.noContent().build();
    }

    /** Copia una semana sobre otra: la mayoria de los turnos se repiten. */
    @PostMapping("/copy")
    public ScheduleWeekDTO copy(@Valid @RequestBody CopyWeekRequestDTO dto) {
        return scheduleService.copyWeek(dto.getFromMonday(), dto.getToMonday());
    }
}
