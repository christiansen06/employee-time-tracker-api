package com.tuusuario.employee_time_tracker.Service;

import com.tuusuario.employee_time_tracker.Exception.ResourceNotFoundException;
import com.tuusuario.employee_time_tracker.Model.Entity.Employee;
import com.tuusuario.employee_time_tracker.Model.Entity.User;
import com.tuusuario.employee_time_tracker.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Resuelve la ficha de empleado asociada al usuario autenticado.
 * Usado por los endpoints "/me" para que el empleado fiche su propia jornada
 * sin necesidad de pasar su id.
 */
@Service
@RequiredArgsConstructor
public class CurrentEmployeeService {

    private final UserRepository userRepository;

    public Employee getByUsername(String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + username));

        Employee employee = user.getEmployee();

        if (employee == null) {
            throw new IllegalStateException(
                    "Your user account is not linked to an employee.");
        }

        return employee;
    }
}
