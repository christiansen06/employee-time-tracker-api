package com.tuusuario.employee_time_tracker.Service;

import com.tuusuario.employee_time_tracker.Model.Dto.TimeEntrySummaryDTO;
import com.tuusuario.employee_time_tracker.Model.Entity.Employee;
import com.tuusuario.employee_time_tracker.Model.Entity.TimeEntry;
import com.tuusuario.employee_time_tracker.Model.Enums.TimeEntryStatus;
import com.tuusuario.employee_time_tracker.Repository.BreakEntryRepository;
import com.tuusuario.employee_time_tracker.Repository.EmployeeRepository;
import com.tuusuario.employee_time_tracker.Repository.TimeEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeEntryServiceTest {

    @Mock private TimeEntryRepository timeEntryRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private BreakEntryRepository breakEntryRepository;
    @Mock private CurrentEmployeeService currentEmployeeService;
    @Mock private AuditLogService auditLogService;
    @Mock private PaidPeriodGuard paidPeriodGuard;

    @InjectMocks private TimeEntryService service;

    private Employee employee() {
        return Employee.builder().id(1L).name("Juan").lastName("Perez")
                .email("juan@test.com").position("Cocina").active(true).build();
    }

    @Test
    void clockInCreatesOpenEntry() {
        Employee emp = employee();
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(emp));
        when(timeEntryRepository.existsByEmployeeIdAndStatusIn(eq(1L), anyCollection()))
                .thenReturn(false);
        when(timeEntryRepository.save(any(TimeEntry.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TimeEntrySummaryDTO dto = service.clockIn(1L);

        assertThat(dto.getStatus()).isEqualTo(TimeEntryStatus.CLOCKED_IN);
        assertThat(dto.getClockIn()).isNotNull();
        assertThat(dto.getClockOut()).isNull();
    }

    @Test
    void clockInFailsIfEntryAlreadyOpen() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee()));
        when(timeEntryRepository.existsByEmployeeIdAndStatusIn(eq(1L), anyCollection()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.clockIn(1L))
                .isInstanceOf(IllegalStateException.class);
        verify(timeEntryRepository, never()).save(any());
    }

    @Test
    void clockOutFailsWhileOnBreak() {
        TimeEntry entry = TimeEntry.builder().id(5L)
                .status(TimeEntryStatus.ON_BREAK).employee(employee()).build();
        when(timeEntryRepository.findById(5L)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> service.clockOut(5L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("break");
    }

    @Test
    void clockOutFailsIfAlreadyFinished() {
        TimeEntry entry = TimeEntry.builder().id(5L)
                .status(TimeEntryStatus.FINISHED).employee(employee()).build();
        when(timeEntryRepository.findById(5L)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> service.clockOut(5L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void clockOutFinishesEntry() {
        TimeEntry entry = TimeEntry.builder().id(5L)
                .clockIn(LocalDateTime.now().minusHours(8))
                .status(TimeEntryStatus.CLOCKED_IN).employee(employee()).build();
        when(timeEntryRepository.findById(5L)).thenReturn(Optional.of(entry));
        when(timeEntryRepository.save(any(TimeEntry.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TimeEntrySummaryDTO dto = service.clockOut(5L);

        assertThat(dto.getStatus()).isEqualTo(TimeEntryStatus.FINISHED);
        assertThat(dto.getClockOut()).isNotNull();
    }

    @Test
    void updateEntryRejectsInvertedRange() {
        TimeEntry entry = TimeEntry.builder().id(5L)
                .status(TimeEntryStatus.FINISHED).employee(employee()).build();
        when(timeEntryRepository.findById(5L)).thenReturn(Optional.of(entry));

        LocalDateTime in = LocalDateTime.of(2026, 7, 10, 17, 0);
        LocalDateTime out = LocalDateTime.of(2026, 7, 10, 9, 0);

        assertThatThrownBy(() -> service.updateEntry(5L, in, out))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateEntryRecordsAuditTrail() {
        TimeEntry entry = TimeEntry.builder().id(5L)
                .clockIn(LocalDateTime.of(2026, 7, 10, 8, 0))
                .clockOut(LocalDateTime.of(2026, 7, 10, 16, 0))
                .status(TimeEntryStatus.FINISHED).employee(employee()).build();
        when(timeEntryRepository.findById(5L)).thenReturn(Optional.of(entry));
        when(timeEntryRepository.save(any(TimeEntry.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.updateEntry(5L,
                LocalDateTime.of(2026, 7, 10, 9, 0),
                LocalDateTime.of(2026, 7, 10, 17, 0));

        verify(auditLogService).record(eq("TIME_ENTRY"), eq(5L), eq("UPDATE"), anyString());
    }

    @Test
    void deleteEntryRecordsAuditTrail() {
        TimeEntry entry = TimeEntry.builder().id(5L)
                .status(TimeEntryStatus.FINISHED).employee(employee()).build();
        when(timeEntryRepository.findById(5L)).thenReturn(Optional.of(entry));

        service.deleteEntry(5L);

        verify(auditLogService).record(eq("TIME_ENTRY"), eq(5L), eq("DELETE"), anyString());
        verify(timeEntryRepository).delete(entry);
    }

    @Test
    void createManualEntryCreatesFinishedEntryAndAudits() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee()));
        when(timeEntryRepository.existsByEmployeeIdAndClockInLessThanAndClockOutGreaterThan(
                eq(1L), any(), any())).thenReturn(false);
        when(timeEntryRepository.save(any(TimeEntry.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TimeEntrySummaryDTO dto = service.createManualEntry(1L,
                LocalDateTime.of(2026, 7, 9, 9, 0),
                LocalDateTime.of(2026, 7, 9, 17, 0));

        assertThat(dto.getStatus()).isEqualTo(TimeEntryStatus.FINISHED);
        assertThat(dto.getClockIn()).isEqualTo(LocalDateTime.of(2026, 7, 9, 9, 0));
        assertThat(dto.getClockOut()).isEqualTo(LocalDateTime.of(2026, 7, 9, 17, 0));
        verify(auditLogService).record(eq("TIME_ENTRY"), any(), eq("CREATE"), anyString());
    }

    @Test
    void createManualEntryRejectsInvertedRange() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee()));

        assertThatThrownBy(() -> service.createManualEntry(1L,
                LocalDateTime.of(2026, 7, 9, 17, 0),
                LocalDateTime.of(2026, 7, 9, 9, 0)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(timeEntryRepository, never()).save(any());
    }

    @Test
    void createManualEntryRejectsOverlapWithExistingEntry() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee()));
        when(timeEntryRepository.existsByEmployeeIdAndClockInLessThanAndClockOutGreaterThan(
                eq(1L), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> service.createManualEntry(1L,
                LocalDateTime.of(2026, 7, 9, 9, 0),
                LocalDateTime.of(2026, 7, 9, 17, 0)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("overlap");
        verify(timeEntryRepository, never()).save(any());
    }

    @Test
    void operationsOnPaidPeriodAreLocked() {
        // Todo el periodo de la jornada esta pagado.
        org.mockito.Mockito.doThrow(new IllegalStateException(
                        "This period was already paid and is locked."))
                .when(paidPeriodGuard).assertNotPaid(eq(1L), any());

        TimeEntry entry = TimeEntry.builder().id(5L)
                .clockIn(LocalDateTime.of(2026, 7, 9, 9, 0))
                .clockOut(LocalDateTime.of(2026, 7, 9, 17, 0))
                .status(TimeEntryStatus.FINISHED).employee(employee()).build();
        when(timeEntryRepository.findById(5L)).thenReturn(Optional.of(entry));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee()));

        assertThatThrownBy(() -> service.updateEntry(5L,
                LocalDateTime.of(2026, 7, 9, 8, 0), LocalDateTime.of(2026, 7, 9, 16, 0)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("paid");
        assertThatThrownBy(() -> service.deleteEntry(5L))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("paid");
        assertThatThrownBy(() -> service.setPaidDouble(5L, true))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("paid");
        assertThatThrownBy(() -> service.createManualEntry(1L,
                LocalDateTime.of(2026, 7, 10, 9, 0), LocalDateTime.of(2026, 7, 10, 17, 0)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("paid");

        verify(timeEntryRepository, never()).save(any());
        verify(timeEntryRepository, never()).delete(any(TimeEntry.class));
    }

    @Test
    void setPaidDoubleMarksEntryAndAudits() {
        TimeEntry entry = TimeEntry.builder().id(5L)
                .clockIn(LocalDateTime.of(2026, 7, 9, 9, 0))
                .clockOut(LocalDateTime.of(2026, 7, 9, 17, 0))
                .status(TimeEntryStatus.FINISHED).employee(employee()).build();
        when(timeEntryRepository.findById(5L)).thenReturn(Optional.of(entry));
        when(timeEntryRepository.save(any(TimeEntry.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TimeEntrySummaryDTO dto = service.setPaidDouble(5L, true);

        assertThat(dto.getPaidDouble()).isTrue();
        verify(auditLogService).record(eq("TIME_ENTRY"), eq(5L), eq("UPDATE"),
                eq("paidDouble: false -> true"));
    }

    @Test
    void setPaidDoubleWithSameValueDoesNothing() {
        TimeEntry entry = TimeEntry.builder().id(5L)
                .paidDouble(true)
                .status(TimeEntryStatus.FINISHED).employee(employee()).build();
        when(timeEntryRepository.findById(5L)).thenReturn(Optional.of(entry));

        TimeEntrySummaryDTO dto = service.setPaidDouble(5L, true);

        assertThat(dto.getPaidDouble()).isTrue();
        verify(timeEntryRepository, never()).save(any());
        verify(auditLogService, never()).record(anyString(), anyLong(), anyString(), anyString());
    }

    @Test
    void clockOutByEmployeeIdFailsWithoutOpenEntry() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee()));
        when(timeEntryRepository.findFirstByEmployeeIdAndStatusInOrderByClockInDesc(
                anyLong(), anyCollection())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.clockOutByEmployeeId(1L))
                .isInstanceOf(IllegalStateException.class);
    }
}
