package com.tuusuario.employee_time_tracker.Service;

import com.tuusuario.employee_time_tracker.Exception.ResourceNotFoundException;
import com.tuusuario.employee_time_tracker.Model.Dto.ScheduleCellDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.ScheduleRowDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.ScheduleWeekDTO;
import com.tuusuario.employee_time_tracker.Model.Entity.Employee;
import com.tuusuario.employee_time_tracker.Model.Entity.ShiftSchedule;
import com.tuusuario.employee_time_tracker.Repository.EmployeeRepository;
import com.tuusuario.employee_time_tracker.Repository.ShiftScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Horario planificado de la semana: lo que el admin arma por adelantado
 * para despues compartirlo con el equipo.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ScheduleService {

    private static final int DAYS_IN_WEEK = 7;

    private final ShiftScheduleRepository shiftScheduleRepository;
    private final EmployeeRepository employeeRepository;
    private final AuditLogService auditLogService;

    // ---------- Lectura ----------

    /**
     * Grilla de la semana: un renglon por empleado activo, con las 7 celdas
     * (null donde no hay turno cargado) y el total planificado.
     */
    @Transactional(readOnly = true)
    public ScheduleWeekDTO getWeek(LocalDate from, LocalDate to) {

        List<LocalDate> days = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            days.add(d);
        }

        // Turnos de la semana, indexados por empleado y fecha.
        Map<Long, Map<LocalDate, ShiftSchedule>> byEmployee =
                shiftScheduleRepository.findByShiftDateBetween(from, to).stream()
                        .collect(Collectors.groupingBy(
                                s -> s.getEmployee().getId(),
                                Collectors.toMap(ShiftSchedule::getShiftDate,
                                        Function.identity(),
                                        (a, b) -> a)));

        List<ScheduleRowDTO> rows = employeeRepository.findByActiveTrue().stream()
                .sorted(Comparator.comparing(Employee::getName,
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Employee::getLastName,
                                String.CASE_INSENSITIVE_ORDER))
                .map(emp -> {
                    Map<LocalDate, ShiftSchedule> ofEmployee =
                            byEmployee.getOrDefault(emp.getId(), Map.of());

                    List<ScheduleCellDTO> cells = new ArrayList<>();
                    long total = 0;
                    for (LocalDate day : days) {
                        ShiftSchedule s = ofEmployee.get(day);
                        ScheduleCellDTO cell = s == null ? null : mapToCell(s);
                        cells.add(cell);
                        if (cell != null) {
                            total += cell.getPlannedMinutes();
                        }
                    }

                    return ScheduleRowDTO.builder()
                            .employeeId(emp.getId())
                            .employeeName(emp.getName() + " " + emp.getLastName())
                            .cells(cells)
                            .plannedMinutes(total)
                            .build();
                })
                .toList();

        return ScheduleWeekDTO.builder()
                .from(from).to(to).days(days).rows(rows).build();
    }

    // ---------- Escritura ----------

    /** Crea o pisa el turno de un empleado en un dia. */
    public ScheduleCellDTO upsertCell(Long employeeId,
                                      LocalDate date,
                                      LocalTime start,
                                      LocalTime end,
                                      boolean dayOff,
                                      String note) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with id: " + employeeId));

        String cleanNote = (note == null || note.isBlank()) ? null : note.trim();

        // Franco manda: no tiene sentido guardarle horarios.
        if (dayOff) {
            start = null;
            end = null;
        }
        if (!dayOff && cleanNote == null && (start == null || end == null)) {
            throw new IllegalArgumentException(
                    "Load a start and end time, mark it as day off, or write a note.");
        }
        if (start != null && end != null && start.equals(end)) {
            throw new IllegalArgumentException("startTime and endTime cannot be equal.");
        }

        Optional<ShiftSchedule> existing =
                shiftScheduleRepository.findByEmployeeIdAndShiftDate(employeeId, date);

        ShiftSchedule shift = existing.orElseGet(() -> ShiftSchedule.builder()
                .employee(employee)
                .shiftDate(date)
                .build());

        String before = existing.map(this::describe).orElse("(vacio)");

        shift.setStartTime(start);
        shift.setEndTime(end);
        shift.setDayOff(dayOff);
        shift.setNote(cleanNote);

        ShiftSchedule saved = shiftScheduleRepository.save(shift);

        auditLogService.record("SCHEDULE", saved.getId(),
                existing.isPresent() ? "UPDATE" : "CREATE",
                "employeeId=" + employeeId + ", date=" + date
                        + " | before: " + before + " | after: " + describe(saved));

        return mapToCell(saved);
    }

    /** Vacia la celda (el empleado queda sin nada ese dia). */
    public void deleteCell(Long employeeId, LocalDate date) {

        ShiftSchedule shift = shiftScheduleRepository
                .findByEmployeeIdAndShiftDate(employeeId, date)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "There is no scheduled shift for that employee on " + date));

        auditLogService.record("SCHEDULE", shift.getId(), "DELETE",
                "employeeId=" + employeeId + ", date=" + date
                        + ", " + describe(shift));

        shiftScheduleRepository.delete(shift);
    }

    /**
     * Copia una semana entera sobre otra (pisando lo que hubiera): la mayoria
     * de los turnos se repiten, asi no hay que recargarlos a mano.
     */
    public ScheduleWeekDTO copyWeek(LocalDate fromMonday, LocalDate toMonday) {

        if (fromMonday.equals(toMonday)) {
            throw new IllegalArgumentException("Source and target weeks must be different.");
        }

        LocalDate fromSunday = fromMonday.plusDays(DAYS_IN_WEEK - 1L);
        LocalDate toSunday = toMonday.plusDays(DAYS_IN_WEEK - 1L);
        long offsetDays = Duration.between(
                fromMonday.atStartOfDay(), toMonday.atStartOfDay()).toDays();

        List<ShiftSchedule> source = shiftScheduleRepository
                .findByShiftDateBetween(fromMonday, fromSunday);

        if (source.isEmpty()) {
            throw new IllegalStateException("The week you are copying from is empty.");
        }

        shiftScheduleRepository.deleteByShiftDateBetween(toMonday, toSunday);
        shiftScheduleRepository.flush();

        List<ShiftSchedule> copies = source.stream()
                .map(s -> ShiftSchedule.builder()
                        .employee(s.getEmployee())
                        .shiftDate(s.getShiftDate().plusDays(offsetDays))
                        .startTime(s.getStartTime())
                        .endTime(s.getEndTime())
                        .dayOff(s.getDayOff())
                        .note(s.getNote())
                        .build())
                .toList();

        shiftScheduleRepository.saveAll(copies);

        auditLogService.record("SCHEDULE", null, "COPY",
                "week " + fromMonday + " -> " + toMonday
                        + " (" + copies.size() + " turnos)");

        return getWeek(toMonday, toSunday);
    }

    // ---------- Mapeo ----------

    private ScheduleCellDTO mapToCell(ShiftSchedule s) {
        long minutes = plannedMinutes(s);
        return ScheduleCellDTO.builder()
                .id(s.getId())
                .date(s.getShiftDate())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .dayOff(Boolean.TRUE.equals(s.getDayOff()))
                .note(s.getNote())
                .plannedMinutes(minutes)
                .label(buildLabel(s))
                .build();
    }

    /** Un turno que termina antes de empezar cruza la medianoche. */
    private long plannedMinutes(ShiftSchedule s) {
        if (Boolean.TRUE.equals(s.getDayOff())
                || s.getStartTime() == null || s.getEndTime() == null) {
            return 0;
        }
        long minutes = Duration.between(s.getStartTime(), s.getEndTime()).toMinutes();
        return minutes > 0 ? minutes : minutes + Duration.ofDays(1).toMinutes();
    }

    /** "9 a 19", "9:30 a 13", "Franco", "Mossa". */
    private String buildLabel(ShiftSchedule s) {
        if (Boolean.TRUE.equals(s.getDayOff())) {
            return "Franco";
        }
        if (s.getStartTime() == null || s.getEndTime() == null) {
            return s.getNote() == null ? "" : s.getNote();
        }
        String range = shortTime(s.getStartTime()) + " a " + shortTime(s.getEndTime());
        return s.getNote() == null ? range : range + " · " + s.getNote();
    }

    /** Sin minutos cuando son en punto, como se escribia en la planilla. */
    private String shortTime(LocalTime t) {
        return t.getMinute() == 0
                ? String.valueOf(t.getHour())
                : String.format("%d:%02d", t.getHour(), t.getMinute());
    }

    private String describe(ShiftSchedule s) {
        String label = buildLabel(s);
        return label.isEmpty() ? "(vacio)" : label;
    }
}
