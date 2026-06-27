package com.tuusuario.employee_time_tracker.Controller;

import com.tuusuario.employee_time_tracker.Model.Dto.BreakEndRequestDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.BreakResponseDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.BreakStartRequestDTO;
import com.tuusuario.employee_time_tracker.Service.BreakService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/breaks")
@RequiredArgsConstructor
public class BreakController {

    private final BreakService breakService;

    // ---------- ADMIN: operar el break de cualquier empleado por id ----------

    @PostMapping("/start")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BreakResponseDTO> startBreak(@RequestBody BreakStartRequestDTO dto) {
        return ResponseEntity.ok(breakService.startBreak(dto));
    }

    @PostMapping("/end")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BreakResponseDTO> endBreak(@RequestBody BreakEndRequestDTO dto) {
        return ResponseEntity.ok(breakService.endBreak(dto));
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BreakResponseDTO>> getActiveBreaks() {
        return ResponseEntity.ok(breakService.getActiveBreaks());
    }

    // ---------- SELF: el empleado autenticado opera su propio break ----------

    @PostMapping("/me/start")
    public ResponseEntity<BreakResponseDTO> startBreakMe(Authentication authentication) {
        return ResponseEntity.ok(breakService.startBreakCurrent(authentication.getName()));
    }

    @PostMapping("/me/end")
    public ResponseEntity<BreakResponseDTO> endBreakMe(Authentication authentication) {
        return ResponseEntity.ok(breakService.endBreakCurrent(authentication.getName()));
    }
}
