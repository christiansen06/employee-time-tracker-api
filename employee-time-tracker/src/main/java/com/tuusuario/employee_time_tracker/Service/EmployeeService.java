package com.tuusuario.employee_time_tracker.Service;

import com.tuusuario.employee_time_tracker.Model.Dto.DailyHoursDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.EmployeeRequestDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.EmployeeResponseDTO;
import com.tuusuario.employee_time_tracker.Exception.DuplicateResourceException;
import com.tuusuario.employee_time_tracker.Exception.ResourceNotFoundException;
import com.tuusuario.employee_time_tracker.Model.Dto.WeeklyHoursDetailDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.WorkIntervalDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.WorkedHoursResponseDTO;
import com.tuusuario.employee_time_tracker.Model.Entity.BreakEntry;
import com.tuusuario.employee_time_tracker.Model.Entity.Employee;
import com.tuusuario.employee_time_tracker.Model.Entity.TimeEntry;
import com.tuusuario.employee_time_tracker.Model.Enums.TimeEntryStatus;
import com.tuusuario.employee_time_tracker.Repository.EmployeeRepository;
import com.tuusuario.employee_time_tracker.Util.WorkTimeCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        if (employeeRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "An employee with email " + request.getEmail() + " already exists.");
        }

        Employee employee = Employee.builder()
                .name(request.getName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .position(request.getPosition())
                .hourlyRate(request.getHourlyRate())
                .expectedClockIn(request.getExpectedClockIn())
                .weeklyHoursTarget(request.getWeeklyHoursTarget())
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

        if (employeeRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new DuplicateResourceException(
                    "An employee with email " + request.getEmail() + " already exists.");
        }

        existingEmployee.setName(request.getName());
        existingEmployee.setLastName(request.getLastName());
        existingEmployee.setEmail(request.getEmail());
        existingEmployee.setPosition(request.getPosition());
        existingEmployee.setHourlyRate(request.getHourlyRate());
        existingEmployee.setExpectedClockIn(request.getExpectedClockIn());
        existingEmployee.setWeeklyHoursTarget(request.getWeeklyHoursTarget());

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

        // Reusa el desglose semanal (mismo criterio de semana Lun-Dom en
        // todos lados, e incluye fichadas del lunes 00:00 exacto).
        WeeklyHoursDetailDTO detail = getWeeklyDetail(employeeId);

        long totalWorkedMinutes = detail.getTotalWorkedMinutes();
        long overtimeMinutes = Math.max(0, totalWorkedMinutes - (40 * 60));

        Employee employee = getEmployeeEntity(employeeId);

        return WorkedHoursResponseDTO.builder()
                .employeeId(employee.getId())
                .employeeName(employee.getName() + " " + employee.getLastName())
                .totalWorkedMinutes(totalWorkedMinutes)
                .totalWorkedHours(totalWorkedMinutes / 60.0)
                .overtimeMinutes(overtimeMinutes)
                .overtimeHours(overtimeMinutes / 60.0)
                .build();
    }


    /**
     * Desglose dia a dia (lunes a domingo) de la semana actual,
     * neto de breaks. Para la tarjeta semanal del kiosco.
     */
    public WeeklyHoursDetailDTO getWeeklyDetail(Long employeeId) {
        return getWeeklyDetail(employeeId, LocalDate.now());
    }

    /**
     * Desglose dia a dia de la semana que contiene la fecha ancla.
     * Permite consultar semanas pasadas (liquidacion).
     */
    public WeeklyHoursDetailDTO getWeeklyDetail(Long employeeId, LocalDate anchor) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with id: " + employeeId));

        LocalDate weekStart = anchor.with(DayOfWeek.MONDAY);
        LocalDateTime from = weekStart.atStartOfDay();
        LocalDateTime to = weekStart.plusDays(7).atStartOfDay();

        Map<LocalDate, List<TimeEntry>> entriesPerDay = new HashMap<>();

        if (employee.getTimeEntries() != null) {
            employee.getTimeEntries().stream()
                    .filter(e -> e.getStatus() == TimeEntryStatus.FINISHED)
                    .filter(e -> e.getClockIn() != null && e.getClockOut() != null)
                    .filter(e -> !e.getClockIn().isBefore(from) && e.getClockIn().isBefore(to))
                    .forEach(e -> entriesPerDay
                            .computeIfAbsent(e.getClockIn().toLocalDate(), d -> new ArrayList<>())
                            .add(e));
        }

        DateTimeFormatter hm = DateTimeFormatter.ofPattern("HH:mm");
        List<DailyHoursDTO> days = new ArrayList<>();
        long total = 0;

        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);
            List<TimeEntry> dayEntries = new ArrayList<>(entriesPerDay.getOrDefault(day, List.of()));
            dayEntries.sort(java.util.Comparator.comparing(TimeEntry::getClockIn));

            long minutes = 0;
            List<WorkIntervalDTO> intervals = new ArrayList<>();
            for (TimeEntry e : dayEntries) {
                minutes += entryNetMinutes(e);
                intervals.add(WorkIntervalDTO.builder()
                        .clockIn(e.getClockIn().format(hm))
                        .clockOut(e.getClockOut().format(hm))
                        .breakMinutes(entryBreakMinutes(e))
                        .build());
            }

            total += minutes;
            days.add(DailyHoursDTO.builder()
                    .date(day)
                    .workedMinutes(minutes)
                    .intervals(intervals)
                    .build());
        }

        return WeeklyHoursDetailDTO.builder()
                .employeeId(employee.getId())
                .employeeName(employee.getName() + " " + employee.getLastName())
                .weekStart(weekStart)
                .weekEnd(weekStart.plusDays(6))
                .days(days)
                .totalWorkedMinutes(total)
                .totalWorkedHours(total / 60.0)
                .build();
    }

    /** Minutos netos de una jornada: duracion menos breaks. */
    private long entryNetMinutes(TimeEntry entry) {
        return WorkTimeCalculator.netMinutes(entry);
    }

    /** Total de minutos de break de una jornada. */
    private long entryBreakMinutes(TimeEntry entry) {
        return WorkTimeCalculator.breakMinutes(entry);
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
                .hasPin(employee.getPinHash() != null)
                .hourlyRate(employee.getHourlyRate())
                .expectedClockIn(employee.getExpectedClockIn())
                .weeklyHoursTarget(employee.getWeeklyHoursTarget())
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
