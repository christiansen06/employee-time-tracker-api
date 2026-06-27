package com.tuusuario.employee_time_tracker.Model.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SetPinRequestDTO {

    @NotBlank(message = "PIN is required")
    @Pattern(regexp = "\\d{4,6}", message = "PIN must be 4 to 6 digits")
    private String pin;
}
