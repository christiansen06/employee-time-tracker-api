package com.tuusuario.employee_time_tracker.Service;

import com.tuusuario.employee_time_tracker.Exception.DuplicateResourceException;
import com.tuusuario.employee_time_tracker.Exception.ResourceNotFoundException;
import com.tuusuario.employee_time_tracker.Model.Dto.BonusDayDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.BonusDayRequestDTO;
import com.tuusuario.employee_time_tracker.Model.Entity.BonusDay;
import com.tuusuario.employee_time_tracker.Model.Entity.Employee;
import com.tuusuario.employee_time_tracker.Repository.BonusDayRepository;
import com.tuusuario.employee_time_tracker.Repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BonusDayServiceTest {

    @Mock private BonusDayRepository bonusDayRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private PaidPeriodGuard paidPeriodGuard;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private BonusDayService service;

    private final LocalDate day = LocalDate.of(2026, 11, 7);
    private final Employee ana = Employee.builder().id(1L).name("Ana").lastName("Lopez").build();
    private final Employee beto = Employee.builder().id(2L).name("Beto").lastName("Diaz").build();

    private BonusDayRequestDTO request(Long... ids) {
        BonusDayRequestDTO dto = new BonusDayRequestDTO();
        dto.setDate(day);
        dto.setAmountPerHour(new BigDecimal("1000"));
        dto.setNote("  Food truck + local ");
        dto.setEmployeeIds(Set.of(ids));
        return dto;
    }

    @Test
    void createStoresDayWithChosenEmployees() {
        when(bonusDayRepository.existsByBonusDate(day)).thenReturn(false);
        when(employeeRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(ana, beto));
        when(bonusDayRepository.save(any(BonusDay.class))).thenAnswer(inv -> {
            BonusDay b = inv.getArgument(0);
            b.setId(5L);
            return b;
        });

        BonusDayDTO dto = service.create(request(1L, 2L));

        assertThat(dto.getId()).isEqualTo(5L);
        assertThat(dto.getNote()).isEqualTo("Food truck + local");
        assertThat(dto.getEmployeeNames()).containsExactly("Ana Lopez", "Beto Diaz");
        verify(paidPeriodGuard).assertNotPaid(1L, day);
        verify(paidPeriodGuard).assertNotPaid(2L, day);
    }

    @Test
    void createRejectsSecondBonusOnSameDay() {
        when(bonusDayRepository.existsByBonusDate(day)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request(1L)))
                .isInstanceOf(DuplicateResourceException.class);
        verify(bonusDayRepository, never()).save(any());
    }

    @Test
    void createRejectsUnknownEmployee() {
        when(bonusDayRepository.existsByBonusDate(day)).thenReturn(false);
        when(employeeRepository.findAllById(Set.of(1L, 99L))).thenReturn(List.of(ana));

        assertThatThrownBy(() -> service.create(request(1L, 99L)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createRejectsDayInsideAPaidPeriod() {
        when(bonusDayRepository.existsByBonusDate(day)).thenReturn(false);
        when(employeeRepository.findAllById(Set.of(1L))).thenReturn(List.of(ana));
        doThrow(new IllegalStateException("locked")).when(paidPeriodGuard).assertNotPaid(1L, day);

        assertThatThrownBy(() -> service.create(request(1L)))
                .isInstanceOf(IllegalStateException.class);
        verify(bonusDayRepository, never()).save(any());
    }

    @Test
    void deleteIsBlockedWhenAnIncludedEmployeeWasAlreadyPaid() {
        BonusDay existing = BonusDay.builder().id(5L).bonusDate(day)
                .amountPerHour(new BigDecimal("1000"))
                .employees(new HashSet<>(Set.of(ana))).build();
        when(bonusDayRepository.findById(5L)).thenReturn(Optional.of(existing));
        doThrow(new IllegalStateException("locked")).when(paidPeriodGuard).assertNotPaid(1L, day);

        assertThatThrownBy(() -> service.delete(5L)).isInstanceOf(IllegalStateException.class);
        verify(bonusDayRepository, never()).delete(any());
    }

    @Test
    void updateChecksBothOldAndNewEmployeesAgainstPaidPeriods() {
        BonusDay existing = BonusDay.builder().id(5L).bonusDate(day)
                .amountPerHour(new BigDecimal("1000"))
                .employees(new HashSet<>(Set.of(ana))).build();
        when(bonusDayRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(bonusDayRepository.existsByBonusDateAndIdNot(day, 5L)).thenReturn(false);
        when(employeeRepository.findAllById(Set.of(2L))).thenReturn(List.of(beto));
        when(bonusDayRepository.save(any(BonusDay.class))).thenAnswer(inv -> inv.getArgument(0));

        BonusDayDTO dto = service.update(5L, request(2L));

        verify(paidPeriodGuard).assertNotPaid(1L, day); // quien se quita
        verify(paidPeriodGuard).assertNotPaid(2L, day); // quien se agrega
        assertThat(dto.getEmployeeIds()).containsExactly(2L);
    }
}
