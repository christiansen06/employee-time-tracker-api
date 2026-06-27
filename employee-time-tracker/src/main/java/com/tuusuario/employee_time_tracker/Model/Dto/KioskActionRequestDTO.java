package com.tuusuario.employee_time_tracker.Model.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Pedido del kiosco: que empleado y su PIN para autorizar la accion. */
@Data
public class KioskActionRequestDTO {

    @NotNull(message = "employeeId is required")
    private Long employeeId;

    @NotBlank(message = "PIN is required")
    private String pin;
}
