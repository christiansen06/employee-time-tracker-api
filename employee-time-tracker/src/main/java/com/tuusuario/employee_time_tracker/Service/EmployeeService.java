package com.tuusuario.employee_time_tracker.Service;

import com.tuusuario.employee_time_tracker.Model.Dto.EmployeeRequestDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.EmployeeResponseDTO;
import com.tuusuario.employee_time_tracker.Exception.ResourceNotFoundException;
import com.tuusuario.employee_time_tracker.Model.Dto.WorkedHoursResponseDTO;
import com.tuusuario.employee_time_tracker.Model.Entity.BreakEntry;
import com.tuusuario.employee_time_tracker.Model.Entity.Employee;
import com.tuusuario.employee_time_tracker.Model.Entity.TimeEntry;
import com.tuusuario.employee_time_tracker.Model.Enums.TimeEntryStatus;
import com.tuusuario.employee_time_tracker.Repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final CurrentEmployeeService currentEmployeeService;
    private final PasswordEncoder passwordEncoder;

    /** Asigna (o reemplaza) el PIN de fichaje del empleado, hasheado. */
    public void setPin(Long id, String rawPin) {
        Employee employee = getEmployeeEntity(id);
        employee.setPinHash(passwordEncoder.encode(rawPin));
        employeeRepository.save(employee);
    }

    // ----- Horas del usuario autenticado (para los botones "/me") -----

    /** Horas trabajadas (netas de breaks) en la semana actual, del usuario logueado. */
    public WorkedHoursResponseDTO getWeeklyWorkedHoursForCurrentUser(String username) {
        Employee employee = currentEmployeeService.getByUsername(username);
        return getWeeklyWorkedHours(employee.getId());
    }

    /** Horas trabajadas (netas de breaks) histórico total, del usuario logueado. */
    public WorkedHoursResponseDTO getWorkedHoursForCurrentUser(String username) {
        Employee employee = currentEmployeeService.getByUsername(username);
        return getWorkedHours(employee.getId());
    }

    //CREATE
    public EmployeeResponseDTO createEmployee(EmployeeRequestDTO request) {
        Employee employee = Employee.builder()
                .name(request.getName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .position(request.getPosition())
                .active(true)
                .build();

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
        return toResponse(getEmployeeEntity(id));
    }

    //UPDATE
    public EmployeeResponseDTO updateEmployee(Long id, EmployeeRequestDTO request) {
        Employee existingEmployee = getEmployeeEntity(id);

        existingEmployee.setName(request.getName());
        existingEmployee.setLastName(request.getLastName());
        existingEmployee.setEmail(request.getEmail());
        existingEmployee.setPosition(request.getPosition());

        return toResponse(employeeRepository.save(existingEmployee));
    }

    //DELETE
    public void deactivateEmployee(Long id) {
        Employee employee = getEmployeeEntity(id);
        employee.setActive(false);
        employeeRepository.save(employee);
    }

    //ACTIVATE
    public EmployeeResponseDTO activateEmployee(Long id) {
        Employee employee = getEmployeeEntity(id);

        employee.setActive(true);

        return toResponse(employeeRepository.save(employee));
    }

    //Total Historico Horas
    public WorkedHoursResponseDTO getWorkedHours(Long employeeId) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee not found with id: " + employeeId));

        List<TimeEntry> finishedEntries = employee.getTimeEntries().stream()
                .filter(entry -> entry.getStatus() == TimeEntryStatus.FINISHED)
                .filter(entry -> entry.getClockIn() != null)
                .filter(entry -> entry.getClockOut() != null)
                .toList();

        long totalWorkedMinutes = calculateWorkedMinutes(finishedEntries);
        long overtimeMinutes = Math.max(0, totalWorkedMinutes - (40 * 60)); // Asumiendo 40 horas semanales

        return WorkedHoursResponseDTO.builder()
                .employeeId(employee.getId())
                .employeeName(employee.getName() + " " + employee.getLastName())
                .totalWorkedMinutes(totalWorkedMinutes)
                .totalWorkedHours(totalWorkedMinutes /60.0)
                .overtimeMinutes(overtimeMinutes)
                .overtimeHours(overtimeMinutes / 60.0)
                .build();
    }

    //Horas Semanales
    public WorkedHoursResponseDTO getWeeklyWorkedHours(Long employeeId) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with id: " + employeeId));

        LocalDateTime startOfWeek = LocalDate.now()
                .with(DayOfWeek.MONDAY)
                .atStartOfDay();

        List<TimeEntry> weeklyEntries = employee.getTimeEntries()
                .stream()
                .filter(entry -> entry.getStatus() == TimeEntryStatus.FINISHED)
                .filter(entry -> entry.getClockIn() != null)
                .filter(entry -> entry.getClockOut() != null)
                .filter(entry -> entry.getClockIn().isAfter(startOfWeek))
                .toList();

        long totalWorkedMinutes = calculateWorkedMinutes(weeklyEntries);

        long overtimeMinutes = Math.max(0, totalWorkedMinutes - (40 * 60));

        return WorkedHoursResponseDTO.builder()
                .employeeId(employee.getId())
                .employeeName(employee.getName() + " " + employee.getLastName())
                .totalWorkedMinutes(totalWorkedMinutes)
                .totalWorkedHours(totalWorkedMinutes / 60.0)
                .overtimeMinutes(overtimeMinutes)
                .overtimeHours(overtimeMinutes / 60.0)
                .build();
    }


    //Metodos Auxiliares
    //Buscar Empleado por id
    private Employee getEmployeeEntity(Long id) {
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

    private long calculateWorkedMinutes(List<TimeEntry> entries) {
        return entries.stream()
                .mapToLong(entry -> {
                    long workedMinutes = Duration.between(
                            entry.getClockIn(),
                            entry.getClockOut()
                    ).toMinutes();

                    long breakMinutes = 0;
                    if (entry.getBreaks() != null) {
                        breakMinutes = entry.getBreaks().stream()
                                .filter(b -> b.getDurationMinutes() != null)
                                .mapToLong(BreakEntry::getDurationMinutes)
                                .sum();
                    }

                    return workedMinutes - breakMinutes;
                })
                .sum();
    }
}
