package com.tuusuario.employee_time_tracker.Dto;

import com.tuusuario.employee_time_tracker.Enums.Role;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequestDTO {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    private Role role;
}
