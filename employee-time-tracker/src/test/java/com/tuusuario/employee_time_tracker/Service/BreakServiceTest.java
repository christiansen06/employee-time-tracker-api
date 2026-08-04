package com.tuusuario.employee_time_tracker.Service;

import com.tuusuario.employee_time_tracker.Model.Dto.BreakResponseDTO;
import com.tuusuario.employee_time_tracker.Model.Entity.BreakEntry;
import com.tuusuario.employee_time_tracker.Model.Entity.Employee;
import com.tuusuario.employee_time_tracker.Model.Entity.TimeEntry;
import com.tuusuario.employee_time_tracker.Model.Enums.BreakStatus;
import com.tuusuario.employee_time_tracker.Model.Enums.TimeEntryStatus;
import com.tuusuario.employee_time_tracker.Repository.BreakEntryRepository;
import com.tuusuario.employee_time_tracker.Repository.TimeEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BreakServiceTest {

    @Mock private BreakEntryRepository breakEntryRepository;
    @Mock private TimeEntryRepository timeEntryRepository;
    @Mock private CurrentEmployeeService currentEmployeeService;
    @Mock private AuditLogService auditLogService;
    @Mock private PaidPeriodGuard paidPeriodGuard;

    @InjectMocks private BreakService service;

    private TimeEntry openEntry() {
        return TimeEntry.builder().id(10L)
                .clockIn(LocalDateTime.now().minusHours(2))
                .status(TimeEntryStatus.CLOCKED_IN)
                .employee(Employee.builder().id(1L).build())
                .breaks(new ArrayList<>())
                .build();
    }

    @Test
    void startBreakFailsWithoutOpenEntry() {
        when(timeEntryRepository.findFirstByEmployeeIdAndStatusInOrderByClockInDesc(
                anyLong(), anyCollection())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.startBreakByEmployeeId(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Clock in");
    }

    @Test
    void startBreakFailsIfAlreadyOnBreak() {
        TimeEntry entry = openEntry();
        entry.setStatus(TimeEntryStatus.ON_BREAK);
        entry.getBreaks().add(BreakEntry.builder()
                .breakStatus(BreakStatus.ON_BREAK).build());
        when(timeEntryRepository.findFirstByEmployeeIdAndStatusInOrderByClockInDesc(
                anyLong(), anyCollection())).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> service.startBreakByEmployeeId(1L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void startBreakMarksEntryOnBreak() {
        TimeEntry entry = openEntry();
        when(timeEntryRepository.findFirstByEmployeeIdAndStatusInOrderByClockInDesc(
                anyLong(), anyCollection())).thenReturn(Optional.of(entry));
        when(timeEntryRepository.save(any(TimeEntry.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(breakEntryRepository.save(any(BreakEntry.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        BreakResponseDTO dto = service.startBreakByEmployeeId(1L);

        assertThat(dto.getBreakStatus()).isEqualTo(BreakStatus.ON_BREAK);
        assertThat(entry.getStatus()).isEqualTo(TimeEntryStatus.ON_BREAK);
    }

    // ---------- ABM manual del admin ----------

    /** Jornada cerrada 9-17 con un break existente 13:00-13:30. */
    private TimeEntry closedEntryWithBreak() {
        TimeEntry entry = TimeEntry.builder().id(10L)
                .clockIn(LocalDateTime.of(2026, 7, 9, 9, 0))
                .clockOut(LocalDateTime.of(2026, 7, 9, 17, 0))
                .status(TimeEntryStatus.FINISHED)
                .employee(Employee.builder().id(1L).build())
                .breaks(new ArrayList<>())
                .build();
        entry.getBreaks().add(BreakEntry.builder().id(20L)
                .breakStart(LocalDateTime.of(2026, 7, 9, 13, 0))
                .breakEnd(LocalDateTime.of(2026, 7, 9, 13, 30))
                .durationMinutes(30L)
                .breakStatus(BreakStatus.FINISHED)
                .timeEntry(entry)
                .build());
        return entry;
    }

    @Test
    void updateBreakRecalculatesDurationAndAudits() {
        TimeEntry entry = closedEntryWithBreak();
        BreakEntry target = entry.getBreaks().get(0);
        when(breakEntryRepository.findById(20L)).thenReturn(Optional.of(target));
        when(breakEntryRepository.save(any(BreakEntry.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        BreakResponseDTO dto = service.updateBreak(20L,
                LocalDateTime.of(2026, 7, 9, 13, 0),
                LocalDateTime.of(2026, 7, 9, 14, 15));

        assertThat(dto.getDurationMinutes()).isEqualTo(75L);
        assertThat(dto.getBreakStatus()).isEqualTo(BreakStatus.FINISHED);
        verify(auditLogService).record(eq("BREAK"), eq(20L), eq("UPDATE"), anyString());
    }

    @Test
    void closingForgottenBreakReturnsShiftToWorking() {
        // El empleado volvio a trabajar y se olvido de cerrar el break.
        // La jornada empezo hace 2 hs; el break, hace 90 minutos.
        TimeEntry entry = openEntry();
        entry.setStatus(TimeEntryStatus.ON_BREAK);
        BreakEntry open = BreakEntry.builder().id(21L)
                .breakStart(LocalDateTime.now().minusMinutes(90))
                .breakStatus(BreakStatus.ON_BREAK)
                .timeEntry(entry)
                .build();
        entry.getBreaks().add(open);

        when(breakEntryRepository.findById(21L)).thenReturn(Optional.of(open));
        when(breakEntryRepository.save(any(BreakEntry.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(breakEntryRepository.findFirstByTimeEntry_Employee_IdAndBreakStatus(
                1L, BreakStatus.ON_BREAK)).thenReturn(Optional.empty());
        when(timeEntryRepository.save(any(TimeEntry.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.updateBreak(21L,
                LocalDateTime.now().minusMinutes(90),
                LocalDateTime.now().minusMinutes(60));

        assertThat(entry.getStatus()).isEqualTo(TimeEntryStatus.CLOCKED_IN);
    }

    @Test
    void updateBreakRejectsEndBeforeStart() {
        TimeEntry entry = closedEntryWithBreak();
        when(breakEntryRepository.findById(20L)).thenReturn(Optional.of(entry.getBreaks().get(0)));

        assertThatThrownBy(() -> service.updateBreak(20L,
                LocalDateTime.of(2026, 7, 9, 14, 0),
                LocalDateTime.of(2026, 7, 9, 13, 0)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(breakEntryRepository, never()).save(any());
    }

    @Test
    void breakCannotFallOutsideTheShift() {
        TimeEntry entry = closedEntryWithBreak();
        when(timeEntryRepository.findById(10L)).thenReturn(Optional.of(entry));

        // Empieza antes de la entrada
        assertThatThrownBy(() -> service.createManualBreak(10L,
                LocalDateTime.of(2026, 7, 9, 8, 0),
                LocalDateTime.of(2026, 7, 9, 8, 30)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("before the shift");

        // Termina despues de la salida
        assertThatThrownBy(() -> service.createManualBreak(10L,
                LocalDateTime.of(2026, 7, 9, 16, 30),
                LocalDateTime.of(2026, 7, 9, 18, 0)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("after the shift");
    }

    @Test
    void createManualBreakRejectsOverlapWithExistingBreak() {
        TimeEntry entry = closedEntryWithBreak(); // ya tiene 13:00-13:30
        when(timeEntryRepository.findById(10L)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> service.createManualBreak(10L,
                LocalDateTime.of(2026, 7, 9, 13, 15),
                LocalDateTime.of(2026, 7, 9, 14, 0)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("overlaps");
        verify(breakEntryRepository, never()).save(any());
    }

    @Test
    void createManualBreakAddsClosedBreak() {
        TimeEntry entry = closedEntryWithBreak();
        when(timeEntryRepository.findById(10L)).thenReturn(Optional.of(entry));
        when(breakEntryRepository.save(any(BreakEntry.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        BreakResponseDTO dto = service.createManualBreak(10L,
                LocalDateTime.of(2026, 7, 9, 15, 0),
                LocalDateTime.of(2026, 7, 9, 15, 20));

        assertThat(dto.getDurationMinutes()).isEqualTo(20L);
        assertThat(dto.getBreakStatus()).isEqualTo(BreakStatus.FINISHED);
        verify(auditLogService).record(eq("BREAK"), any(), eq("CREATE"), anyString());
    }

    @Test
    void deleteBreakRemovesItAndAudits() {
        TimeEntry entry = closedEntryWithBreak();
        BreakEntry target = entry.getBreaks().get(0);
        when(breakEntryRepository.findById(20L)).thenReturn(Optional.of(target));

        service.deleteBreak(20L);

        verify(breakEntryRepository).delete(target);
        verify(auditLogService).record(eq("BREAK"), eq(20L), eq("DELETE"), anyString());
    }

    @Test
    void breaksOfAPaidPeriodAreLocked() {
        TimeEntry entry = closedEntryWithBreak();
        BreakEntry target = entry.getBreaks().get(0);
        when(breakEntryRepository.findById(20L)).thenReturn(Optional.of(target));
        doThrow(new IllegalStateException("This period was already paid and is locked."))
                .when(paidPeriodGuard).assertNotPaid(eq(1L), any());

        assertThatThrownBy(() -> service.updateBreak(20L,
                LocalDateTime.of(2026, 7, 9, 13, 0),
                LocalDateTime.of(2026, 7, 9, 14, 0)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("paid");
        assertThatThrownBy(() -> service.deleteBreak(20L))
                .isInstanceOf(IllegalStateException.class);
        verify(breakEntryRepository, never()).save(any());
        verify(breakEntryRepository, never()).delete(any(BreakEntry.class));
    }

    @Test
    void endBreakFailsWithoutActiveBreak() {
        when(breakEntryRepository.findFirstByTimeEntry_Employee_IdAndBreakStatus(
                1L, BreakStatus.ON_BREAK)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.endBreakByEmployeeId(1L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void endBreakComputesDurationAndReopensEntry() {
        TimeEntry entry = openEntry();
        entry.setStatus(TimeEntryStatus.ON_BREAK);
        BreakEntry breakEntry = BreakEntry.builder()
                .id(20L)
                .breakStart(LocalDateTime.now().minusMinutes(30))
                .breakStatus(BreakStatus.ON_BREAK)
                .timeEntry(entry)
                .build();
        when(breakEntryRepository.findFirstByTimeEntry_Employee_IdAndBreakStatus(
                1L, BreakStatus.ON_BREAK)).thenReturn(Optional.of(breakEntry));
        when(timeEntryRepository.save(any(TimeEntry.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(breakEntryRepository.save(any(BreakEntry.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        BreakResponseDTO dto = service.endBreakByEmployeeId(1L);

        assertThat(dto.getBreakStatus()).isEqualTo(BreakStatus.FINISHED);
        assertThat(dto.getDurationMinutes()).isBetween(29L, 31L);
        assertThat(entry.getStatus()).isEqualTo(TimeEntryStatus.CLOCKED_IN);
    }
}
