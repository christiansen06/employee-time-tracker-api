package com.tuusuario.employee_time_tracker.Model.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/** Correccion del horario de un break por parte del ADMIN. */
@Data
public class UpdateBreakRequestDTO {

    @NotNull(message = "breakStart is required")
    private LocalDateTime breakStart;

    @NotNull(message = "breakEnd is required")
    private LocalDateTime breakEnd;
}
