package com.tuusuario.employee_time_tracker.Controller;

import com.tuusuario.employee_time_tracker.Dto.EmployeeRequestDTO;
import com.tuusuario.employee_time_tracker.Model.Employee;
import com.tuusuario.employee_time_tracker.Service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeService employeeService;

    //CREATE
    @PostMapping
    public ResponseEntity<Employee> createEmployee(
            @Valid @RequestBody EmployeeRequestDTO dto) {

        Employee employee = Employee.builder()
                .name(dto.getName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .position(dto.getPosition())
                .active(true)
                .build();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(employeeService.createEmployee(employee));
    }

    //READ ALL
    @GetMapping
    public List<Employee> getAllEmployees() {
        return employeeService.getAllEmployees();
    }

    //READ BY ID
    @GetMapping("/{id}")
    public Employee getEmployeeById(@PathVariable Long id) {
        return employeeService.getEmployeeById(id);
    }

    //UPDATE
    @PutMapping("/{id}")
    public Employee updateEmployee(@PathVariable Long id, @RequestBody Employee employee) {
        return employeeService.updateEmployee(id, employee);
    }

    //SOFT DELETE
    @PatchMapping("/{id}/deactivate")
    public void deactivateEmployee(@PathVariable Long id) {
        employeeService.deactivateEmployee(id);
    }

    //RE-ACTIVATE
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Employee> activateEmployee(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.activateEmployee(id));
    }
}
