package com.tuusuario.employee_time_tracker.Model.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class AuthResponseDTO {

    private String token;

    /** Token opaco para renovar el access token sin reloguear. */
    private String refreshToken;
}
