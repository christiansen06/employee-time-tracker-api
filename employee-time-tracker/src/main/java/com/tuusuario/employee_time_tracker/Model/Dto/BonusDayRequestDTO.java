package com.tuusuario.employee_time_tracker.Model.Dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Data
public class BonusDayRequestDTO {

    @NotNull(message = "date is required")
    private LocalDate date;

    @NotNull(message = "amountPerHour is required")
    @DecimalMin(value = "0.01", message = "amountPerHour must be greater than 0")
    private BigDecimal amountPerHour;

    @Size(max = 120, message = "note must be at most 120 characters")
    private String note;

    @NotEmpty(message = "Choose at least one employee")
    private Set<Long> employeeIds;
}
