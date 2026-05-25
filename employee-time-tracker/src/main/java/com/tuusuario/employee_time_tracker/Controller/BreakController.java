package com.tuusuario.employee_time_tracker.Controller;

import com.tuusuario.employee_time_tracker.Model.Dto.BreakEndRequestDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.BreakResponseDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.BreakStartRequestDTO;
import com.tuusuario.employee_time_tracker.Service.BreakService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/breaks")
public class BreakController {
    private final BreakService breakService;

    public BreakController(BreakService breakService) {
        this.breakService = breakService;
    }

    @PostMapping("/start")
    public ResponseEntity<BreakResponseDTO> startBreak(@RequestBody BreakStartRequestDTO dto) {
        return ResponseEntity.ok(breakService.startBreak(dto));
    }

    @PostMapping("/end")
    public ResponseEntity<BreakResponseDTO> endBreak(@RequestBody BreakEndRequestDTO dto) {
        return ResponseEntity.ok(breakService.endBreak(dto));
    }

    @GetMapping("/active")
    public ResponseEntity<List<BreakResponseDTO>> getActiveBreaks() {
        return ResponseEntity.ok(breakService.getActiveBreaks());
    }
}
