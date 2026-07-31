package com.tuusuario.employee_time_tracker.Security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // Publicos: pagina web, login y documentacion.
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/admin",
                                "/admin.html",
                                "/favicon.ico",
                                "/icon.svg",
                                "/manifest.json",
                                "/assets/**",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/health",
                                "/actuator/health",
                                "/actuator/health/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        // Alta de usuarios: solo ADMIN (tambien reforzado con @PreAuthorize).
                        .requestMatchers("/api/auth/register").hasRole("ADMIN")
                        // Dispositivo del local (kiosco): rol KIOSK o ADMIN.
                        .requestMatchers("/api/kiosk/**").hasAnyRole("KIOSK", "ADMIN")
                        // Gestion de empleados y reportes: solo ADMIN.
                        .requestMatchers("/api/employees/**").hasRole("ADMIN")
                        .requestMatchers("/api/analytics/**").hasRole("ADMIN")
                        .requestMatchers("/api/payments/**").hasRole("ADMIN")
                        // Fichaje y breaks: ADMIN o EMPLOYEE.
                        .requestMatchers("/api/time-entries/**", "/api/breaks/**")
                                .hasAnyRole("ADMIN", "EMPLOYEE")
                        .anyRequest().authenticated()
                )
                // Sesion vencida/invalida -> 401 (el cliente renueva o vuelve
                // a loguear); rol insuficiente -> 403. Ambos con cuerpo JSON.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .addFilterBefore(jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }
}