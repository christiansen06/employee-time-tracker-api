package com.tuusuario.employee_time_tracker.Model.Dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
public class EmployeeRequestDTO {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Email(message = "Invalid email")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Position is required")
    private String position;

    /** Valor hora (opcional) para estimar costo laboral. */
    @DecimalMin(value = "0.0", message = "hourlyRate cannot be negative")
    private BigDecimal hourlyRate;

    /** Hora esperada de entrada (opcional), formato HH:mm. */
    private LocalTime expectedClockIn;

    /** Tope semanal de horas antes de overtime (opcional). */
    @Min(value = 1, message = "weeklyHoursTarget must be at least 1")
    @Max(value = 168, message = "weeklyHoursTarget cannot exceed 168")
    private Integer weeklyHoursTarget;
}
