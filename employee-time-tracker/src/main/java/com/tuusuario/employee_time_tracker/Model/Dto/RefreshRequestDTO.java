package com.tuusuario.employee_time_tracker.Model.Dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RefreshRequestDTO {

    @NotBlank(message = "refreshToken is required")
    private String refreshToken;
}
