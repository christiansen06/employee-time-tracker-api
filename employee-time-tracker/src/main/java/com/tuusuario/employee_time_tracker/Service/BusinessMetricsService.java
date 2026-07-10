package com.tuusuario.employee_time_tracker.Service;

import com.tuusuario.employee_time_tracker.Model.Dto.AbsenceDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.AnalyticsSummaryDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.EmployeeHoursDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.OvertimeDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.PunctualityDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.TrendPointDTO;
import com.tuusuario.employee_time_tracker.Model.Entity.Employee;
import com.tuusuario.employee_time_tracker.Model.Entity.TimeEntry;
import com.tuusuario.employee_time_tracker.Repository.EmployeeRepository;
import com.tuusuario.employee_time_tracker.Repository.TimeEntryRepository;
import com.tuusuario.employee_time_tracker.Util.WorkTimeCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Metricas de negocio sobre un rango de fechas: horas, costo laboral,
 * puntualidad, horas extra, ausentismo y tendencias.
 * Sin rango explicito se analizan los ultimos 30 dias.
 */
@Service
@RequiredArgsConstructor
public class BusinessMetricsService {

    private final TimeEntryRepository timeEntryRepository;
    private final EmployeeRepository employeeRepository;

    /** Minutos de gracia antes de contar una llegada como tarde. */
    @Value("${app.analytics.late-tolerance-minutes:10}")
    private int lateToleranceMinutes;

    /** Horas por dia antes de contar overtime diario. */
    @Value("${app.analytics.daily-overtime-threshold-hours:8}")
    private int dailyOvertimeThresholdHours;

    /** Tope semanal por defecto (48 hs: jornada legal argentina). */
    @Value("${app.analytics.default-weekly-hours-target:48}")
    private int defaultWeeklyHoursTarget;

    private record Range(LocalDate from, LocalDate to) {
    }

    // ---------- Resumen ejecutivo ----------

    public AnalyticsSummaryDTO getSummary(LocalDate from, LocalDate to) {
        Range range = resolveRange(from, to);
        List<TimeEntry> entries = countableEntries(range);
        List<Employee> activeEmployees = employeeRepository.findByActiveTrue();

        Map<Long, List<TimeEntry>> byEmployee = entries.stream()
                .collect(Collectors.groupingBy(e -> e.getEmployee().getId()));

        // Todos los activos aparecen (aun con 0 hs) + cualquiera que haya
        // trabajado en el rango aunque hoy este inactivo.
        Map<Long, Employee> employees = new LinkedHashMap<>();
        activeEmployees.forEach(e -> employees.put(e.getId(), e));
        entries.forEach(e -> employees.putIfAbsent(
                e.getEmployee().getId(), e.getEmployee()));

        List<EmployeeHoursDTO> perEmployee = new ArrayList<>();
        long totalWorked = 0;
        long totalBreaks = 0;
        long totalDaysWorked = 0;
        BigDecimal totalCost = BigDecimal.ZERO;
        int withRate = 0;

        for (Employee emp : employees.values()) {
            List<TimeEntry> own = byEmployee.getOrDefault(emp.getId(), List.of());
            long worked = own.stream().mapToLong(WorkTimeCalculator::netMinutes).sum();
            long breaks = own.stream().mapToLong(WorkTimeCalculator::breakMinutes).sum();
            int days = (int) own.stream()
                    .map(e -> e.getClockIn().toLocalDate()).distinct().count();

            BigDecimal cost = null;
            if (emp.getHourlyRate() != null) {
                withRate++;
                cost = costOf(worked, emp.getHourlyRate());
                totalCost = totalCost.add(cost);
            }

            totalWorked += worked;
            totalBreaks += breaks;
            totalDaysWorked += days;

            perEmployee.add(EmployeeHoursDTO.builder()
                    .employeeId(emp.getId())
                    .employeeName(fullName(emp))
                    .workedMinutes(worked)
                    .workedHours(round2(worked / 60.0))
                    .breakMinutes(breaks)
                    .daysWorked(days)
                    .estimatedCost(cost)
                    .build());
        }

        perEmployee.sort(Comparator.comparingLong(EmployeeHoursDTO::getWorkedMinutes).reversed());

        double avgShift = totalDaysWorked > 0
                ? round2(totalWorked / 60.0 / totalDaysWorked) : 0;

        return AnalyticsSummaryDTO.builder()
                .from(range.from())
                .to(range.to())
                .activeEmployees(activeEmployees.size())
                .totalWorkedMinutes(totalWorked)
                .totalWorkedHours(round2(totalWorked / 60.0))
                .totalBreakMinutes(totalBreaks)
                .avgHoursPerEmployeePerDay(avgShift)
                .estimatedLaborCost(totalCost.setScale(2, RoundingMode.HALF_UP))
                .employeesWithHourlyRate(withRate)
                .perEmployee(perEmployee)
                .build();
    }

    // ---------- Puntualidad ----------

    public List<PunctualityDTO> getPunctuality(LocalDate from, LocalDate to) {
        Range range = resolveRange(from, to);

        // Para puntualidad alcanza con la hora de entrada: cuentan tambien
        // las jornadas todavia abiertas.
        Map<Long, List<TimeEntry>> byEmployee = entriesInRange(range).stream()
                .filter(e -> e.getClockIn() != null)
                .collect(Collectors.groupingBy(e -> e.getEmployee().getId()));

        List<PunctualityDTO> result = new ArrayList<>();

        for (Employee emp : employeeRepository.findByActiveTrue()) {
            if (emp.getExpectedClockIn() == null) {
                continue; // sin hora esperada no se mide
            }

            Map<LocalDate, LocalTime> firstArrivalPerDay =
                    byEmployee.getOrDefault(emp.getId(), List.of()).stream()
                            .collect(Collectors.toMap(
                                    e -> e.getClockIn().toLocalDate(),
                                    e -> e.getClockIn().toLocalTime(),
                                    (a, b) -> a.isBefore(b) ? a : b));

            LocalTime limit = emp.getExpectedClockIn()
                    .plusMinutes(lateToleranceMinutes);

            List<Long> lateMinutes = firstArrivalPerDay.values().stream()
                    .filter(arrival -> arrival.isAfter(limit))
                    .map(arrival -> (long) ChronoUnit.MINUTES.between(
                            emp.getExpectedClockIn(), arrival))
                    .toList();

            int evaluated = firstArrivalPerDay.size();
            int late = lateMinutes.size();

            result.add(PunctualityDTO.builder()
                    .employeeId(emp.getId())
                    .employeeName(fullName(emp))
                    .expectedClockIn(emp.getExpectedClockIn())
                    .daysEvaluated(evaluated)
                    .lateArrivals(late)
                    .latePercentage(evaluated > 0
                            ? round2(late * 100.0 / evaluated) : 0)
                    .avgLateMinutes(late > 0
                            ? round2(lateMinutes.stream()
                                    .mapToLong(Long::longValue).average().orElse(0))
                            : 0)
                    .build());
        }

        result.sort(Comparator.comparingDouble(PunctualityDTO::getLatePercentage).reversed());
        return result;
    }

    // ---------- Horas extra ----------

    public List<OvertimeDTO> getOvertime(LocalDate from, LocalDate to) {
        Range range = resolveRange(from, to);
        Map<Long, List<TimeEntry>> byEmployee = countableEntries(range).stream()
                .collect(Collectors.groupingBy(e -> e.getEmployee().getId()));

        long dailyThreshold = dailyOvertimeThresholdHours * 60L;
        List<OvertimeDTO> result = new ArrayList<>();

        for (List<TimeEntry> own : byEmployee.values()) {
            Employee emp = own.get(0).getEmployee();

            Map<LocalDate, Long> minutesPerDay = own.stream()
                    .collect(Collectors.groupingBy(
                            e -> e.getClockIn().toLocalDate(),
                            Collectors.summingLong(WorkTimeCalculator::netMinutes)));

            long dailyOvertime = minutesPerDay.values().stream()
                    .mapToLong(m -> Math.max(0, m - dailyThreshold))
                    .sum();

            int target = emp.getWeeklyHoursTarget() != null
                    ? emp.getWeeklyHoursTarget() : defaultWeeklyHoursTarget;

            // Semana ISO (lunes a domingo), consistente con el reporte semanal.
            Map<String, Long> minutesPerWeek = minutesPerDay.entrySet().stream()
                    .collect(Collectors.groupingBy(
                            e -> e.getKey().get(IsoFields.WEEK_BASED_YEAR) + "-"
                                    + e.getKey().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR),
                            Collectors.summingLong(Map.Entry::getValue)));

            long weeklyOvertime = minutesPerWeek.values().stream()
                    .mapToLong(m -> Math.max(0, m - target * 60L))
                    .sum();

            result.add(OvertimeDTO.builder()
                    .employeeId(emp.getId())
                    .employeeName(fullName(emp))
                    .dailyOvertimeMinutes(dailyOvertime)
                    .weeklyOvertimeMinutes(weeklyOvertime)
                    .weeklyHoursTarget(target)
                    .dailyOvertimeHours(round2(dailyOvertime / 60.0))
                    .weeklyOvertimeHours(round2(weeklyOvertime / 60.0))
                    .build());
        }

        result.sort(Comparator.comparingLong(
                (OvertimeDTO o) -> o.getDailyOvertimeMinutes() + o.getWeeklyOvertimeMinutes())
                .reversed());
        return result;
    }

    // ---------- Ausentismo ----------

    public List<AbsenceDTO> getAbsences(LocalDate from, LocalDate to) {
        Range range = resolveRange(from, to);
        List<TimeEntry> entries = entriesInRange(range).stream()
                .filter(e -> e.getClockIn() != null)
                .toList();

        // Dias en que el local estuvo operativo: alguien ficho ese dia.
        Set<LocalDate> openDays = entries.stream()
                .map(e -> e.getClockIn().toLocalDate())
                .filter(d -> !d.isAfter(LocalDate.now()))
                .collect(Collectors.toCollection(TreeSet::new));

        Map<Long, Set<LocalDate>> workedDaysByEmployee = entries.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getEmployee().getId(),
                        Collectors.mapping(e -> e.getClockIn().toLocalDate(),
                                Collectors.toSet())));

        List<AbsenceDTO> result = new ArrayList<>();

        for (Employee emp : employeeRepository.findByActiveTrue()) {
            Set<LocalDate> worked = workedDaysByEmployee
                    .getOrDefault(emp.getId(), Set.of());

            List<LocalDate> absent = openDays.stream()
                    .filter(d -> !worked.contains(d))
                    .toList();

            result.add(AbsenceDTO.builder()
                    .employeeId(emp.getId())
                    .employeeName(fullName(emp))
                    .businessOpenDays(openDays.size())
                    .daysWorked(worked.size())
                    .daysAbsent(absent.size())
                    .absencePercentage(!openDays.isEmpty()
                            ? round2(absent.size() * 100.0 / openDays.size()) : 0)
                    .absentDates(absent)
                    .build());
        }

        result.sort(Comparator.comparingInt(AbsenceDTO::getDaysAbsent).reversed());
        return result;
    }

    // ---------- Tendencia diaria ----------

    public List<TrendPointDTO> getTrends(LocalDate from, LocalDate to) {
        Range range = resolveRange(from, to);
        Map<LocalDate, List<TimeEntry>> byDay = countableEntries(range).stream()
                .collect(Collectors.groupingBy(e -> e.getClockIn().toLocalDate()));

        List<TrendPointDTO> series = new ArrayList<>();
        for (LocalDate day = range.from(); !day.isAfter(range.to()); day = day.plusDays(1)) {
            List<TimeEntry> dayEntries = byDay.getOrDefault(day, List.of());
            long minutes = dayEntries.stream()
                    .mapToLong(WorkTimeCalculator::netMinutes).sum();
            int people = (int) dayEntries.stream()
                    .map(e -> e.getEmployee().getId()).distinct().count();

            series.add(TrendPointDTO.builder()
                    .date(day)
                    .workedMinutes(minutes)
                    .workedHours(round2(minutes / 60.0))
                    .employeesWorked(people)
                    .build());
        }
        return series;
    }

    // ---------- Exportes CSV (separador ';' + BOM, como el reporte semanal) ----------

    public String buildSummaryCsv(LocalDate from, LocalDate to) {
        AnalyticsSummaryDTO summary = getSummary(from, to);
        StringBuilder sb = new StringBuilder("﻿");
        sb.append("Empleado;Dias trabajados;Minutos;Horas;Breaks (min);Costo estimado\n");
        for (EmployeeHoursDTO row : summary.getPerEmployee()) {
            sb.append(row.getEmployeeName()).append(";")
                    .append(row.getDaysWorked()).append(";")
                    .append(row.getWorkedMinutes()).append(";")
                    .append(formatMinutes(row.getWorkedMinutes())).append(";")
                    .append(row.getBreakMinutes()).append(";")
                    .append(row.getEstimatedCost() != null ? row.getEstimatedCost() : "")
                    .append("\n");
        }
        sb.append("TOTAL;;").append(summary.getTotalWorkedMinutes()).append(";")
                .append(formatMinutes(summary.getTotalWorkedMinutes())).append(";")
                .append(summary.getTotalBreakMinutes()).append(";")
                .append(summary.getEstimatedLaborCost()).append("\n");
        return sb.toString();
    }

    public String buildOvertimeCsv(LocalDate from, LocalDate to) {
        List<OvertimeDTO> rows = getOvertime(from, to);
        StringBuilder sb = new StringBuilder("﻿");
        sb.append("Empleado;Extra diaria (min);Extra diaria (hs);")
                .append("Extra semanal (min);Extra semanal (hs);Tope semanal (hs)\n");
        for (OvertimeDTO row : rows) {
            sb.append(row.getEmployeeName()).append(";")
                    .append(row.getDailyOvertimeMinutes()).append(";")
                    .append(formatMinutes(row.getDailyOvertimeMinutes())).append(";")
                    .append(row.getWeeklyOvertimeMinutes()).append(";")
                    .append(formatMinutes(row.getWeeklyOvertimeMinutes())).append(";")
                    .append(row.getWeeklyHoursTarget()).append("\n");
        }
        return sb.toString();
    }

    // ---------- Helpers ----------

    private Range resolveRange(LocalDate from, LocalDate to) {
        LocalDate effectiveTo = to != null ? to : LocalDate.now();
        LocalDate effectiveFrom = from != null ? from : effectiveTo.minusDays(29);

        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new IllegalArgumentException("from cannot be after to.");
        }
        if (ChronoUnit.DAYS.between(effectiveFrom, effectiveTo) > 366) {
            throw new IllegalArgumentException("Range cannot exceed one year.");
        }
        return new Range(effectiveFrom, effectiveTo);
    }

    private List<TimeEntry> entriesInRange(Range range) {
        return timeEntryRepository.findByClockInBetween(
                range.from().atStartOfDay(),
                range.to().plusDays(1).atStartOfDay());
    }

    private List<TimeEntry> countableEntries(Range range) {
        return entriesInRange(range).stream()
                .filter(WorkTimeCalculator::isCountable)
                .toList();
    }

    private BigDecimal costOf(long minutes, BigDecimal hourlyRate) {
        return hourlyRate.multiply(BigDecimal.valueOf(minutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private String fullName(Employee employee) {
        return employee.getName() + " " + employee.getLastName();
    }

    private String formatMinutes(long minutes) {
        return minutes / 60 + ":" + String.format("%02d", minutes % 60);
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
