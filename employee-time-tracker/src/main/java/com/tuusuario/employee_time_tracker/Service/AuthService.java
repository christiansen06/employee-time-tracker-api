package com.tuusuario.employee_time_tracker.Service;

import com.tuusuario.employee_time_tracker.Exception.DuplicateResourceException;
import com.tuusuario.employee_time_tracker.Exception.ResourceNotFoundException;
import com.tuusuario.employee_time_tracker.Model.Dto.AuthRequestDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.AuthResponseDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.RegisterRequestDTO;
import com.tuusuario.employee_time_tracker.Model.Entity.Employee;
import com.tuusuario.employee_time_tracker.Model.Entity.User;
import com.tuusuario.employee_time_tracker.Model.Enums.Role;
import com.tuusuario.employee_time_tracker.Repository.EmployeeRepository;
import com.tuusuario.employee_time_tracker.Repository.UserRepository;
import com.tuusuario.employee_time_tracker.Util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /** Duracion del token del dispositivo kiosco (default 30 dias). */
    @Value("${jwt.kiosk-expiration-ms:2592000000}")
    private long kioskExpirationMs;

    public String register(RegisterRequestDTO request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException(
                    "Username already exists: " + request.getUsername());
        }

        Employee employee = resolveEmployee(request);

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .employee(employee)
                .build();

        userRepository.save(user);

        return "User registered successfully";
    }

    /**
     * Resuelve y valida la ficha de empleado a vincular con la cuenta.
     * EMPLOYEE requiere employeeId; el empleado debe existir y no estar ya
     * vinculado a otra cuenta.
     */
    private Employee resolveEmployee(RegisterRequestDTO request) {

        if (request.getRole() == Role.EMPLOYEE && request.getEmployeeId() == null) {
            throw new IllegalArgumentException(
                    "employeeId is required when role is EMPLOYEE.");
        }

        if (request.getEmployeeId() == null) {
            return null;
        }

        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with id: " + request.getEmployeeId()));

        if (userRepository.existsByEmployee_Id(employee.getId())) {
            throw new DuplicateResourceException(
                    "Employee with id " + employee.getId()
                            + " is already linked to a user account.");
        }

        return employee;
    }

    public AuthResponseDTO login(AuthRequestDTO request) {

        // Mensaje generico e identico en ambos casos (usuario inexistente o
        // password incorrecta) para no filtrar si el usuario existe.
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        boolean passwordMatches =
                passwordEncoder.matches(request.getPassword(), user.getPassword());

        if (!passwordMatches) {
            throw new BadCredentialsException("Invalid username or password");
        }

        // El dispositivo kiosco recibe un token de larga duracion para no
        // tener que reconfigurarse cada hora.
        long ttl = user.getRole() == Role.KIOSK
                ? kioskExpirationMs
                : jwtUtil.getDefaultExpirationMs();

        String token = jwtUtil.generateToken(user.getUsername(), ttl);

        return new AuthResponseDTO(token);
    }
}
