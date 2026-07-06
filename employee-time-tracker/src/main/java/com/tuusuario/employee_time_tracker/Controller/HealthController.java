package com.tuusuario.employee_time_tracker.Controller;

import com.tuusuario.employee_time_tracker.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint publico y liviano para el keep-alive (cron-job.org).
 * Hace una consulta trivial a la base para mantener despiertos
 * tanto el servidor (Render) como la base de datos (Neon).
 */
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final UserRepository userRepository;

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "db", userRepository.count() >= 0
        );
    }
}
