package com.tuusuario.employee_time_tracker.Security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuusuario.employee_time_tracker.Model.Dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Respuesta cuando el pedido NO esta autenticado (token ausente, vencido
 * o invalido): 401 con cuerpo JSON.
 *
 * Sin esto Spring usa Http403ForbiddenEntryPoint y devuelve 403 con cuerpo
 * vacio, indistinguible de "autenticado pero sin permisos". El kiosco
 * quedaba mostrando "Error 403" en vez de renovar el token o volver a la
 * pantalla de configuracion del dispositivo.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        // Marca para que el cliente distinga "sesion vencida" (hay que renovar
        // el token) de un 401 de negocio, como un PIN incorrecto.
        response.setHeader("X-Session-Expired", "true");

        objectMapper.writeValue(response.getWriter(), ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message("Your session expired. Please sign in again.")
                .path(request.getRequestURI())
                .build());
    }
}
