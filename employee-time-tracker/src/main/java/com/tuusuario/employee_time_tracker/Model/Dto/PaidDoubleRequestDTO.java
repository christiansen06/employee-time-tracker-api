package com.tuusuario.employee_time_tracker.Model.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Marca (o desmarca) una jornada como pagada al doble (feriado). */
@Data
public class PaidDoubleRequestDTO {

    @NotNull(message = "paidDouble is required")
    private Boolean paidDouble;
}
