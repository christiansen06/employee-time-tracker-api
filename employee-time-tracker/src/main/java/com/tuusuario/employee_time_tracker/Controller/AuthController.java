package com.tuusuario.employee_time_tracker.Controller;

import com.tuusuario.employee_time_tracker.Model.Dto.AuthRequestDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.AuthResponseDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.RefreshRequestDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.RegisterRequestDTO;
import com.tuusuario.employee_time_tracker.Service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequestDTO request) {

        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
            @Valid @RequestBody AuthRequestDTO request) {

        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDTO> refresh(
            @Valid @RequestBody RefreshRequestDTO request) {

        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
    }
}
