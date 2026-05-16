package com.tuusuario.employee_time_tracker.Controller;

import com.tuusuario.employee_time_tracker.Dto.AuthRequestDTO;
import com.tuusuario.employee_time_tracker.Dto.AuthResponseDTO;
import com.tuusuario.employee_time_tracker.Dto.RegisterRequestDTO;
import com.tuusuario.employee_time_tracker.Service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequestDTO request) {

        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
            @RequestBody AuthRequestDTO request) {

        return ResponseEntity.ok(authService.login(request));
    }
}
