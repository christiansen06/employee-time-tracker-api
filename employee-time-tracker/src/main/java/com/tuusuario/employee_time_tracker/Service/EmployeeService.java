package com.tuusuario.employee_time_tracker.Service;

import com.tuusuario.employee_time_tracker.Model.Employee;
import com.tuusuario.employee_time_tracker.Repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeRepository employeeRepository;

    //CREATE
    public Employee createEmployee(Employee employee) {

        if(employee.getActive() == null) {
            employee.setActive(true);
        }

        return employeeRepository.save(employee);
    }

    //READ ALL
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    //READ BY ID
    public Employee getEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(()->
                        new RuntimeException("Employee not found with id: "+ id));
    }

    //UPDATE
    public Employee updateEmployee(Long id, Employee updatedEmployee) {
        Employee existingEmployee = getEmployeeById(id);

        existingEmployee.setName(updatedEmployee.getName());
        existingEmployee.setLastName(updatedEmployee.getLastName());
        existingEmployee.setEmail(updatedEmployee.getEmail());
        existingEmployee.setPosition(updatedEmployee.getPosition());

        return employeeRepository.save(existingEmployee);
    }

    //DELETE
    public void desactivateEmployee(Long id) {
        Employee employee = getEmployeeById(id);
        employee.setActive(false);
        employeeRepository.save(employee);
    }
}
