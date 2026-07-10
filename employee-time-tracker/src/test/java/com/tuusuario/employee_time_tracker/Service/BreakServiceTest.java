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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BreakServiceTest {

    @Mock private BreakEntryRepository breakEntryRepository;
    @Mock private TimeEntryRepository timeEntryRepository;
    @Mock private CurrentEmployeeService currentEmployeeService;

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
