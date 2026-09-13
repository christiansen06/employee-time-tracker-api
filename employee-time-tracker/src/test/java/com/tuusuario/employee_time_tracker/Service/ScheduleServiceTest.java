package com.tuusuario.employee_time_tracker.Service;

import com.tuusuario.employee_time_tracker.Model.Dto.ScheduleCellDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.ScheduleWeekDTO;
import com.tuusuario.employee_time_tracker.Model.Entity.Employee;
import com.tuusuario.employee_time_tracker.Model.Entity.ShiftSchedule;
import com.tuusuario.employee_time_tracker.Repository.EmployeeRepository;
import com.tuusuario.employee_time_tracker.Repository.ShiftScheduleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 9, 7);
    private static final LocalDate SUNDAY = LocalDate.of(2026, 9, 13);

    @Mock private ShiftScheduleRepository shiftScheduleRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private ScheduleService service;

    private Employee employee(Long id, String name) {
        return Employee.builder().id(id).name(name).lastName("Test").build();
    }

    private ShiftSchedule shift(Employee e, LocalDate date, String from, String to) {
        return ShiftSchedule.builder()
                .id(date.getDayOfMonth() * 10L + e.getId())
                .employee(e).shiftDate(date)
                .startTime(LocalTime.parse(from)).endTime(LocalTime.parse(to))
                .dayOff(false).build();
    }

    @Test
    void weekIncludesEveryActiveEmployeeAndAddsUpPlannedHours() {
        Employee sasha = employee(1L, "Sasha");
        Employee liliana = employee(2L, "Liliana");   // sin turnos, como en la planilla
        when(employeeRepository.findByActiveTrue()).thenReturn(List.of(sasha, liliana));
        when(shiftScheduleRepository.findByShiftDateBetween(MONDAY, SUNDAY))
                .thenReturn(List.of(
                        shift(sasha, MONDAY, "09:00", "19:00"),
                        shift(sasha, MONDAY.plusDays(1), "09:00", "13:00")));

        ScheduleWeekDTO week = service.getWeek(MONDAY, SUNDAY);

        assertThat(week.getDays()).hasSize(7);
        assertThat(week.getRows()).hasSize(2);

        var sashaRow = week.getRows().stream()
                .filter(r -> r.getEmployeeId().equals(1L)).findFirst().orElseThrow();
        assertThat(sashaRow.getCells()).hasSize(7);
        assertThat(sashaRow.getCells().get(0).getLabel()).isEqualTo("9 a 19");
        assertThat(sashaRow.getCells().get(1).getLabel()).isEqualTo("9 a 13");
        assertThat(sashaRow.getCells().get(2)).isNull();
        assertThat(sashaRow.getPlannedMinutes()).isEqualTo(14 * 60);   // 10 hs + 4 hs

        var lilianaRow = week.getRows().stream()
                .filter(r -> r.getEmployeeId().equals(2L)).findFirst().orElseThrow();
        assertThat(lilianaRow.getCells()).containsOnlyNulls();
        assertThat(lilianaRow.getPlannedMinutes()).isZero();
    }

    @Test
    void savingACellComputesLabelAndAudits() {
        Employee sasha = employee(1L, "Sasha");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(sasha));
        when(shiftScheduleRepository.findByEmployeeIdAndShiftDate(1L, MONDAY))
                .thenReturn(Optional.empty());
        when(shiftScheduleRepository.save(any(ShiftSchedule.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ScheduleCellDTO cell = service.upsertCell(1L, MONDAY,
                LocalTime.of(9, 30), LocalTime.of(13, 0), false, null);

        assertThat(cell.getLabel()).isEqualTo("9:30 a 13");
        assertThat(cell.getPlannedMinutes()).isEqualTo(210);
        verify(auditLogService).record(eq("SCHEDULE"), any(), eq("CREATE"), anyString());
    }

    @Test
    void dayOffClearsTheHours() {
        Employee sasha = employee(1L, "Sasha");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(sasha));
        when(shiftScheduleRepository.findByEmployeeIdAndShiftDate(1L, MONDAY))
                .thenReturn(Optional.of(shift(sasha, MONDAY, "09:00", "19:00")));
        when(shiftScheduleRepository.save(any(ShiftSchedule.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ScheduleCellDTO cell = service.upsertCell(1L, MONDAY,
                LocalTime.of(9, 0), LocalTime.of(19, 0), true, null);

        assertThat(cell.isDayOff()).isTrue();
        assertThat(cell.getStartTime()).isNull();
        assertThat(cell.getEndTime()).isNull();
        assertThat(cell.getLabel()).isEqualTo("Franco");
        assertThat(cell.getPlannedMinutes()).isZero();
        verify(auditLogService).record(eq("SCHEDULE"), any(), eq("UPDATE"), anyString());
    }

    @Test
    void aNoteAloneIsEnough() {
        Employee tobias = employee(3L, "Tobias");
        when(employeeRepository.findById(3L)).thenReturn(Optional.of(tobias));
        when(shiftScheduleRepository.findByEmployeeIdAndShiftDate(3L, MONDAY))
                .thenReturn(Optional.empty());
        when(shiftScheduleRepository.save(any(ShiftSchedule.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ScheduleCellDTO cell = service.upsertCell(3L, MONDAY, null, null, false, "Mossa");

        assertThat(cell.getLabel()).isEqualTo("Mossa");
        assertThat(cell.getPlannedMinutes()).isZero();
    }

    @Test
    void anEmptyCellIsRejected() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee(1L, "Sasha")));

        assertThatThrownBy(() -> service.upsertCell(1L, MONDAY, null, null, false, "  "))
                .isInstanceOf(IllegalArgumentException.class);
        verify(shiftScheduleRepository, never()).save(any());
    }

    @Test
    void anOvernightShiftCountsAcrossMidnight() {
        Employee sasha = employee(1L, "Sasha");
        when(employeeRepository.findByActiveTrue()).thenReturn(List.of(sasha));
        when(shiftScheduleRepository.findByShiftDateBetween(MONDAY, SUNDAY))
                .thenReturn(List.of(shift(sasha, MONDAY, "22:00", "06:00")));

        var row = service.getWeek(MONDAY, SUNDAY).getRows().get(0);

        assertThat(row.getCells().get(0).getLabel()).isEqualTo("22 a 6");
        assertThat(row.getPlannedMinutes()).isEqualTo(8 * 60);
    }

    @Test
    void copyWeekShiftsEveryShiftSevenDaysAhead() {
        Employee sasha = employee(1L, "Sasha");
        when(shiftScheduleRepository.findByShiftDateBetween(MONDAY, SUNDAY))
                .thenReturn(List.of(shift(sasha, MONDAY, "09:00", "19:00")));
        when(employeeRepository.findByActiveTrue()).thenReturn(List.of(sasha));

        service.copyWeek(MONDAY, MONDAY.plusDays(7));

        verify(shiftScheduleRepository).deleteByShiftDateBetween(
                MONDAY.plusDays(7), SUNDAY.plusDays(7));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ShiftSchedule>> captor = ArgumentCaptor.forClass(List.class);
        verify(shiftScheduleRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getShiftDate()).isEqualTo(MONDAY.plusDays(7));
        verify(auditLogService).record(eq("SCHEDULE"), any(), eq("COPY"), anyString());
    }

    @Test
    void copyingFromAnEmptyWeekIsRejected() {
        when(shiftScheduleRepository.findByShiftDateBetween(MONDAY, SUNDAY))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.copyWeek(MONDAY, MONDAY.plusDays(7)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("empty");
        verify(shiftScheduleRepository, never()).saveAll(any());
    }

    @Test
    void deletingACellAudits() {
        Employee sasha = employee(1L, "Sasha");
        ShiftSchedule s = shift(sasha, MONDAY, "09:00", "19:00");
        when(shiftScheduleRepository.findByEmployeeIdAndShiftDate(1L, MONDAY))
                .thenReturn(Optional.of(s));

        service.deleteCell(1L, MONDAY);

        verify(shiftScheduleRepository).delete(s);
        verify(auditLogService).record(eq("SCHEDULE"), eq(s.getId()), eq("DELETE"), anyString());
    }
}
