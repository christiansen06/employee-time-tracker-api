package com.tuusuario.employee_time_tracker.Controller;

import com.tuusuario.employee_time_tracker.Model.Dto.CurrentStatusDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.KioskActionRequestDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.KioskEmployeeDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.WeeklyHoursDetailDTO;
import com.tuusuario.employee_time_tracker.Service.KioskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints del dispositivo compartido del local.
 * El acceso lo controla SecurityConfig (rol KIOSK o ADMIN);
 * cada accion exige ademas el PIN del empleado.
 */
@RestController
@RequestMapping("/api/kiosk")
@RequiredArgsConstructor
public class KioskController {

    private final KioskService kioskService;

    @GetMapping("/employees")
    public List<KioskEmployeeDTO> employees() {
        return kioskService.listEmployees();
    }

    @PostMapping("/verify")
    public ResponseEntity<CurrentStatusDTO> verify(@Valid @RequestBody KioskActionRequestDTO dto) {
        return ResponseEntity.ok(kioskService.verify(dto.getEmployeeId(), dto.getPin()));
    }

    @PostMapping("/clock-in")
    public ResponseEntity<CurrentStatusDTO> clockIn(@Valid @RequestBody KioskActionRequestDTO dto) {
        return ResponseEntity.ok(kioskService.clockIn(dto.getEmployeeId(), dto.getPin()));
    }

    @PostMapping("/clock-out")
    public ResponseEntity<CurrentStatusDTO> clockOut(@Valid @RequestBody KioskActionRequestDTO dto) {
        return ResponseEntity.ok(kioskService.clockOut(dto.getEmployeeId(), dto.getPin()));
    }

    @PostMapping("/break/start")
    public ResponseEntity<CurrentStatusDTO> breakStart(@Valid @RequestBody KioskActionRequestDTO dto) {
        return ResponseEntity.ok(kioskService.startBreak(dto.getEmployeeId(), dto.getPin()));
    }

    @PostMapping("/break/end")
    public ResponseEntity<CurrentStatusDTO> breakEnd(@Valid @RequestBody KioskActionRequestDTO dto) {
        return ResponseEntity.ok(kioskService.endBreak(dto.getEmployeeId(), dto.getPin()));
    }

    @PostMapping("/worked-hours")
    public ResponseEntity<WeeklyHoursDetailDTO> workedHours(@Valid @RequestBody KioskActionRequestDTO dto) {
        return ResponseEntity.ok(kioskService.weeklyWorkedHours(dto.getEmployeeId(), dto.getPin()));
    }
}
