package com.tuusuario.employee_time_tracker.Service;

import com.tuusuario.employee_time_tracker.Model.Dto.EmployeeRequestDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.EmployeeResponseDTO;
import com.tuusuario.employee_time_tracker.Exception.ResourceNotFoundException;
import com.tuusuario.employee_time_tracker.Model.Entity.Employee;
import com.tuusuario.employee_time_tracker.Repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeRepository employeeRepository;

    //CREATE
    public EmployeeResponseDTO createEmployee(EmployeeRequestDTO request) {
        Employee employee = Employee.builder()
                .name(request.getName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .position(request.getPosition())
                .active(true)
                .build();

        if(employee.getActive() == null) {
            employee.setActive(true);
        }

        return toResponse(employeeRepository.save(employee));
    }

    //READ ALL
    public List<EmployeeResponseDTO> getAllEmployees() {
        return employeeRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    //READ BY ID
    public EmployeeResponseDTO getEmployeeById(Long id) {
        return toResponse(findEmployeeById(id));
    }

    //UPDATE
    public EmployeeResponseDTO updateEmployee(Long id, EmployeeRequestDTO request) {
        Employee existingEmployee = findEmployeeById(id);

        existingEmployee.setName(request.getName());
        existingEmployee.setLastName(request.getLastName());
        existingEmployee.setEmail(request.getEmail());
        existingEmployee.setPosition(request.getPosition());

        return toResponse(employeeRepository.save(existingEmployee));
    }

    //DELETE
    public void deactivateEmployee(Long id) {
        Employee employee = findEmployeeById(id);
        employee.setActive(false);
        employeeRepository.save(employee);
    }

    public EmployeeResponseDTO activateEmployee(Long id) {
        Employee employee = findEmployeeById(id);

        employee.setActive(true);

        return toResponse(employeeRepository.save(employee));
    }

    private Employee findEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Employee not found with id: " + id));
    }

    private EmployeeResponseDTO toResponse(Employee employee) {
        return EmployeeResponseDTO.builder()
                .id(employee.getId())
                .name(employee.getName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .position(employee.getPosition())
                .active(employee.getActive())
                .build();
    }
}
