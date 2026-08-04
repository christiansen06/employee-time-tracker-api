package com.tuusuario.employee_time_tracker.Model.Dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/** Detalle de un break dentro de una jornada (para la planilla del admin). */
@Value
@Builder
public class BreakDetailDTO {
    /** Necesario para editar o borrar este break puntual. */
    Long id;
    LocalDateTime breakStart;
    LocalDateTime breakEnd;
    Long durationMinutes;
    com.tuusuario.employee_time_tracker.Model.Enums.BreakStatus breakStatus;
}
