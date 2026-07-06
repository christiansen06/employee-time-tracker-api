package com.tuusuario.employee_time_tracker.Service;

import com.tuusuario.employee_time_tracker.Exception.ResourceNotFoundException;
import com.tuusuario.employee_time_tracker.Model.Dto.CurrentStatusDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.KioskEmployeeDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.WeeklyHoursDetailDTO;
import com.tuusuario.employee_time_tracker.Model.Entity.Employee;
import com.tuusuario.employee_time_tracker.Repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Operaciones del dispositivo compartido del local (kiosco).
 * El token del dispositivo (rol KIOSK) ya autoriza el endpoint;
 * el PIN identifica y autentica al empleado en cada accion.
 */
@Service
@RequiredArgsConstructor
public class KioskService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final TimeEntryService timeEntryService;
    private final BreakService breakService;
    private final EmployeeService employeeService;

    /** Empleados activos con su estado actual, para la grilla del kiosco. */
    public List<KioskEmployeeDTO> listEmployees() {
        return employeeRepository.findByActiveTrue().stream()
                .map(e -> KioskEmployeeDTO.builder()
                        .id(e.getId())
                        .fullName(e.getName() + " " + e.getLastName())
                        .state(timeEntryService.getWorkState(e.getId()))
                        .hasPin(e.getPinHash() != null)
                        .build())
                .toList();
    }

    public CurrentStatusDTO verify(Long employeeId, String pin) {
        verifyPin(employeeId, pin);
        return timeEntryService.getStatusByEmployeeId(employeeId);
    }

    public CurrentStatusDTO clockIn(Long employeeId, String pin) {
        verifyPin(employeeId, pin);
        timeEntryService.clockIn(employeeId);
        return timeEntryService.getStatusByEmployeeId(employeeId);
    }

    public CurrentStatusDTO clockOut(Long employeeId, String pin) {
        verifyPin(employeeId, pin);
        timeEntryService.clockOutByEmployeeId(employeeId);
        return timeEntryService.getStatusByEmployeeId(employeeId);
    }

    public CurrentStatusDTO startBreak(Long employeeId, String pin) {
        verifyPin(employeeId, pin);
        breakService.startBreakByEmployeeId(employeeId);
        return timeEntryService.getStatusByEmployeeId(employeeId);
    }

    public CurrentStatusDTO endBreak(Long employeeId, String pin) {
        verifyPin(employeeId, pin);
        breakService.endBreakByEmployeeId(employeeId);
        return timeEntryService.getStatusByEmployeeId(employeeId);
    }

    /** Desglose dia a dia de la semana actual, para que el empleado lo consulte en el kiosco. */
    public WeeklyHoursDetailDTO weeklyWorkedHours(Long employeeId, String pin) {
        verifyPin(employeeId, pin);
        return employeeService.getWeeklyDetail(employeeId);
    }

    private void verifyPin(Long employeeId, String pin) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with id: " + employeeId));

        if (employee.getPinHash() == null) {
            throw new IllegalStateException(
                    "This employee has no PIN configured. Ask the admin to set one.");
        }

        if (!passwordEncoder.matches(pin, employee.getPinHash())) {
            throw new BadCredentialsException("Invalid PIN.");
        }
    }
}
