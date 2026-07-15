package com.tuusuario.employee_time_tracker.Service;

import com.tuusuario.employee_time_tracker.Model.Dto.PaymentDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.PayrollRowDTO;
import com.tuusuario.employee_time_tracker.Model.Entity.Employee;
import com.tuusuario.employee_time_tracker.Model.Entity.Payment;
import com.tuusuario.employee_time_tracker.Model.Entity.TimeEntry;
import com.tuusuario.employee_time_tracker.Model.Enums.TimeEntryStatus;
import com.tuusuario.employee_time_tracker.Repository.EmployeeRepository;
import com.tuusuario.employee_time_tracker.Repository.PaymentRepository;
import com.tuusuario.employee_time_tracker.Repository.TimeEntryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private TimeEntryRepository timeEntryRepository;
    @Mock private BusinessMetricsService businessMetricsService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private PaymentService service;

    private final LocalDate from = LocalDate.of(2026, 7, 6);
    private final LocalDate to = LocalDate.of(2026, 7, 12);

    private Employee employee() {
        return Employee.builder().id(1L).name("Mica").lastName("Gomez")
                .email("mica@test.com").position("Caja").active(true).build();
    }

    private PayrollRowDTO payrollRow() {
        return PayrollRowDTO.builder()
                .employeeId(1L).employeeName("Mica Gomez")
                .workedMinutes(2470).doubleMinutes(606).payableMinutes(3076)
                .workedHours(41.17).doubleHours(10.1).payableHours(51.27)
                .hourlyRate(new BigDecimal("5500"))
                .amount(new BigDecimal("281966.67"))
                .build();
    }

    @Test
    void createSnapshotsPayrollAndAudits() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee()));
        when(paymentRepository
                .existsByEmployeeIdAndFromDateLessThanEqualAndToDateGreaterThanEqual(1L, to, from))
                .thenReturn(false);
        when(timeEntryRepository.findByAutoClosedTrue()).thenReturn(List.of());
        when(businessMetricsService.getPayrollRow(1L, from, to)).thenReturn(payrollRow());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(7L);
            return p;
        });

        PaymentDTO dto = service.create(1L, from, to);

        assertThat(dto.getPayableMinutes()).isEqualTo(3076);
        assertThat(dto.getAmount()).isEqualByComparingTo(new BigDecimal("281966.67"));
        assertThat(dto.getEmployeeName()).isEqualTo("Mica Gomez");
        verify(auditLogService).record(eq("PAYMENT"), eq(7L), eq("CREATE"), anyString());
    }

    @Test
    void createRejectsOverlappingPayment() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee()));
        when(paymentRepository
                .existsByEmployeeIdAndFromDateLessThanEqualAndToDateGreaterThanEqual(1L, to, from))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(1L, from, to))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("overlapping");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createRejectsPeriodWithPendingAutoClosedEntries() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee()));
        when(paymentRepository
                .existsByEmployeeIdAndFromDateLessThanEqualAndToDateGreaterThanEqual(1L, to, from))
                .thenReturn(false);
        when(timeEntryRepository.findByAutoClosedTrue()).thenReturn(List.of(
                TimeEntry.builder().id(9L).employee(employee())
                        .clockIn(from.plusDays(2).atTime(9, 0))
                        .status(TimeEntryStatus.FINISHED).autoClosed(true).build()));

        assertThatThrownBy(() -> service.create(1L, from, to))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("auto-closed");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void createRejectsInvertedPeriod() {
        lenient().when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee()));

        assertThatThrownBy(() -> service.create(1L, to, from))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteReopensPeriodAndAudits() {
        Payment payment = Payment.builder().id(7L).employee(employee())
                .fromDate(from).toDate(to)
                .workedMinutes(100L).doubleMinutes(0L).payableMinutes(100L)
                .amount(new BigDecimal("100")).build();
        when(paymentRepository.findById(7L)).thenReturn(Optional.of(payment));

        service.delete(7L);

        verify(paymentRepository).delete(payment);
        verify(auditLogService).record(eq("PAYMENT"), eq(7L), eq("DELETE"), anyString());
    }
}
