package com.tuusuario.employee_time_tracker.Service;

import com.tuusuario.employee_time_tracker.Model.Dto.AbsenceDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.AnalyticsSummaryDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.OvertimeDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.PunctualityDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.TrendPointDTO;
import com.tuusuario.employee_time_tracker.Model.Entity.Employee;
import com.tuusuario.employee_time_tracker.Model.Entity.TimeEntry;
import com.tuusuario.employee_time_tracker.Model.Enums.TimeEntryStatus;
import com.tuusuario.employee_time_tracker.Repository.EmployeeRepository;
import com.tuusuario.employee_time_tracker.Repository.TimeEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessMetricsServiceTest {

    @Mock private TimeEntryRepository timeEntryRepository;
    @Mock private EmployeeRepository employeeRepository;

    @InjectMocks private BusinessMetricsService service;

    private final LocalDate monday = LocalDate.of(2026, 6, 1); // lunes
    private Employee ana;
    private Employee beto;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "lateToleranceMinutes", 10);
        ReflectionTestUtils.setField(service, "dailyOvertimeThresholdHours", 8);
        ReflectionTestUtils.setField(service, "defaultWeeklyHoursTarget", 48);

        ana = Employee.builder().id(1L).name("Ana").lastName("Lopez")
                .email("ana@test.com").position("Caja").active(true)
                .hourlyRate(new BigDecimal("100.00"))
                .expectedClockIn(LocalTime.of(9, 0))
                .build();
        beto = Employee.builder().id(2L).name("Beto").lastName("Diaz")
                .email("beto@test.com").position("Cocina").active(true)
                .build();
    }

    private TimeEntry shift(Employee emp, LocalDate day, int fromHour, int toHour) {
        return TimeEntry.builder()
                .employee(emp)
                .clockIn(day.atTime(fromHour, 0))
                .clockOut(day.atTime(toHour, 0))
                .status(TimeEntryStatus.FINISHED)
                .build();
    }

    @Test
    void summaryComputesTotalsAndLaborCost() {
        when(timeEntryRepository.findByClockInBetween(any(), any())).thenReturn(List.of(
                shift(ana, monday, 9, 17),           // 8 hs
                shift(ana, monday.plusDays(1), 9, 13), // 4 hs
                shift(beto, monday, 10, 18)          // 8 hs (sin tarifa)
        ));
        when(employeeRepository.findByActiveTrue()).thenReturn(List.of(ana, beto));

        AnalyticsSummaryDTO summary = service.getSummary(monday, monday.plusDays(6));

        assertThat(summary.getTotalWorkedMinutes()).isEqualTo(20 * 60);
        assertThat(summary.getActiveEmployees()).isEqualTo(2);
        assertThat(summary.getEmployeesWithHourlyRate()).isEqualTo(1);
        // Solo Ana tiene tarifa: 12 hs x $100
        assertThat(summary.getEstimatedLaborCost())
                .isEqualByComparingTo(new BigDecimal("1200.00"));
        assertThat(summary.getPerEmployee()).hasSize(2);
        // Promedio de jornada: 20 hs / 3 jornadas-dia
        assertThat(summary.getAvgHoursPerEmployeePerDay()).isEqualTo(6.67);
    }

    @Test
    void punctualityCountsOnlyArrivalsBeyondTolerance() {
        when(timeEntryRepository.findByClockInBetween(any(), any())).thenReturn(List.of(
                shift(ana, monday, 9, 17),                    // 09:00 puntual
                TimeEntry.builder().employee(ana)             // 09:05: dentro de la gracia
                        .clockIn(monday.plusDays(1).atTime(9, 5))
                        .clockOut(monday.plusDays(1).atTime(17, 0))
                        .status(TimeEntryStatus.FINISHED).build(),
                TimeEntry.builder().employee(ana)             // 09:30: tarde (30 min)
                        .clockIn(monday.plusDays(2).atTime(9, 30))
                        .clockOut(monday.plusDays(2).atTime(17, 0))
                        .status(TimeEntryStatus.FINISHED).build()
        ));
        when(employeeRepository.findByActiveTrue()).thenReturn(List.of(ana, beto));

        List<PunctualityDTO> result = service.getPunctuality(monday, monday.plusDays(6));

        // Beto no tiene hora esperada: queda fuera.
        assertThat(result).hasSize(1);
        PunctualityDTO anaP = result.get(0);
        assertThat(anaP.getDaysEvaluated()).isEqualTo(3);
        assertThat(anaP.getLateArrivals()).isEqualTo(1);
        assertThat(anaP.getLatePercentage()).isEqualTo(33.33);
        assertThat(anaP.getAvgLateMinutes()).isEqualTo(30.0);
    }

    @Test
    void overtimeSplitsDailyAndWeeklyExcess() {
        // Ana: lunes 12 hs (4 extra diarias) + resto de la semana 40 hs
        // Total semana: 52 hs -> 4 hs de exceso semanal sobre 48.
        when(timeEntryRepository.findByClockInBetween(any(), any())).thenReturn(List.of(
                shift(ana, monday, 8, 20),               // 12 hs
                shift(ana, monday.plusDays(1), 9, 17),   // 8
                shift(ana, monday.plusDays(2), 9, 17),   // 8
                shift(ana, monday.plusDays(3), 9, 17),   // 8
                shift(ana, monday.plusDays(4), 9, 17),   // 8
                shift(ana, monday.plusDays(5), 9, 17)    // 8
        ));

        List<OvertimeDTO> result = service.getOvertime(monday, monday.plusDays(6));

        assertThat(result).hasSize(1);
        OvertimeDTO ot = result.get(0);
        assertThat(ot.getDailyOvertimeMinutes()).isEqualTo(4 * 60);
        assertThat(ot.getWeeklyOvertimeMinutes()).isEqualTo(4 * 60);
        assertThat(ot.getWeeklyHoursTarget()).isEqualTo(48);
    }

    @Test
    void absencesUseBusinessOpenDays() {
        when(timeEntryRepository.findByClockInBetween(any(), any())).thenReturn(List.of(
                shift(ana, monday, 9, 17),
                shift(ana, monday.plusDays(1), 9, 17),
                shift(beto, monday, 10, 18)
        ));
        when(employeeRepository.findByActiveTrue()).thenReturn(List.of(ana, beto));

        List<AbsenceDTO> result = service.getAbsences(monday, monday.plusDays(6));

        AbsenceDTO betoA = result.stream()
                .filter(a -> a.getEmployeeId().equals(2L)).findFirst().orElseThrow();
        assertThat(betoA.getBusinessOpenDays()).isEqualTo(2);
        assertThat(betoA.getDaysWorked()).isEqualTo(1);
        assertThat(betoA.getDaysAbsent()).isEqualTo(1);
        assertThat(betoA.getAbsentDates()).containsExactly(monday.plusDays(1));
    }

    @Test
    void trendsFillEveryDayOfRange() {
        when(timeEntryRepository.findByClockInBetween(any(), any())).thenReturn(List.of(
                shift(ana, monday, 9, 17)
        ));

        List<TrendPointDTO> series = service.getTrends(monday, monday.plusDays(6));

        assertThat(series).hasSize(7);
        assertThat(series.get(0).getWorkedMinutes()).isEqualTo(480);
        assertThat(series.get(0).getEmployeesWorked()).isEqualTo(1);
        assertThat(series.get(1).getWorkedMinutes()).isZero();
    }

    @Test
    void rangeValidationRejectsInvertedAndHugeRanges() {
        lenient().when(timeEntryRepository.findByClockInBetween(any(), any()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.getSummary(monday, monday.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.getSummary(monday, monday.plusYears(2)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void summaryCsvContainsTotalsRow() {
        when(timeEntryRepository.findByClockInBetween(any(), any())).thenReturn(List.of(
                shift(ana, monday, 9, 17)
        ));
        when(employeeRepository.findByActiveTrue()).thenReturn(List.of(ana));

        String csv = service.buildSummaryCsv(monday, monday.plusDays(6));

        assertThat(csv).contains("Empleado;");
        assertThat(csv).contains("Ana Lopez");
        assertThat(csv).contains("TOTAL;;480;8:00");
    }
}
