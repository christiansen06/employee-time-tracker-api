package com.tuusuario.employee_time_tracker.Controller;

import com.tuusuario.employee_time_tracker.Model.Dto.EmployeeRequestDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.EmployeeResponseDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.SetPinRequestDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.WorkedHoursResponseDTO;
import com.tuusuario.employee_time_tracker.Service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeService employeeService;

    //CREATE
    @PostMapping
    public ResponseEntity<EmployeeResponseDTO> createEmployee(
            @Valid @RequestBody EmployeeRequestDTO dto) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(employeeService.createEmployee(dto));
    }

    //READ ALL
    @GetMapping
    public List<EmployeeResponseDTO> getAllEmployees() {
        return employeeService.getAllEmployees();
    }

    //READ BY ID
    @GetMapping("/{id}")
    public EmployeeResponseDTO getEmployeeById(@PathVariable Long id) {
        return employeeService.getEmployeeById(id);
    }

    //UPDATE
    @PutMapping("/{id}")
    public EmployeeResponseDTO updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeRequestDTO dto) {
        return employeeService.updateEmployee(id, dto);
    }

    //SOFT DELETE
    @PatchMapping("/{id}/deactivate")
    public void deactivateEmployee(@PathVariable Long id) {
        employeeService.deactivateEmployee(id);
    }

    //HARD DELETE (solo empleados sin historial; sino usar deactivate)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    //RE-ACTIVATE
    @PatchMapping("/{id}/activate")
    public ResponseEntity<EmployeeResponseDTO> activateEmployee(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.activateEmployee(id));
    }

    //SET / RESET PIN de fichaje (kiosco)
    @PutMapping("/{id}/pin")
    public ResponseEntity<Void> setPin(
            @PathVariable Long id,
            @Valid @RequestBody SetPinRequestDTO dto) {
        employeeService.setPin(id, dto.getPin());
        return ResponseEntity.noContent().build();
    }

    //Total Historico
    @GetMapping("/{id}/worked-hours")
    public ResponseEntity<WorkedHoursResponseDTO> getWorkedHours(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getWorkedHours(id));
    }

    //Horas Semanales
    @GetMapping("/{id}/worked-hours/week")
    public ResponseEntity<WorkedHoursResponseDTO> getWeeklyWorkedHours(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getWeeklyWorkedHours(id));
    }
}
