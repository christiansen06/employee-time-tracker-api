package com.tuusuario.employee_time_tracker.Model.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/** Copia los turnos de una semana a otra (para no recargar todo a mano). */
@Data
public class CopyWeekRequestDTO {

    @NotNull(message = "fromMonday is required")
    private LocalDate fromMonday;

    @NotNull(message = "toMonday is required")
    private LocalDate toMonday;
}
