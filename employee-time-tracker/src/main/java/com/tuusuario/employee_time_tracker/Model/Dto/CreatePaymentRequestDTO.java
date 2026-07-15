package com.tuusuario.employee_time_tracker.Model.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/** Marca como pagado el periodo [from, to] de un empleado. */
@Data
public class CreatePaymentRequestDTO {

    @NotNull(message = "employeeId is required")
    private Long employeeId;

    @NotNull(message = "from is required")
    private LocalDate from;

    @NotNull(message = "to is required")
    private LocalDate to;
}
