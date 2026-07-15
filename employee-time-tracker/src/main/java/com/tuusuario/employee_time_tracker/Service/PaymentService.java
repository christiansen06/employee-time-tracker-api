package com.tuusuario.employee_time_tracker.Service;

import com.tuusuario.employee_time_tracker.Exception.ResourceNotFoundException;
import com.tuusuario.employee_time_tracker.Model.Dto.PaymentDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.PayrollRowDTO;
import com.tuusuario.employee_time_tracker.Model.Entity.Employee;
import com.tuusuario.employee_time_tracker.Model.Entity.Payment;
import com.tuusuario.employee_time_tracker.Repository.EmployeeRepository;
import com.tuusuario.employee_time_tracker.Repository.PaymentRepository;
import com.tuusuario.employee_time_tracker.Repository.TimeEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Pagos: registran la liquidacion abonada y CIERRAN el periodo del
 * empleado (sus jornadas quedan congeladas hasta reabrir el pago).
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final EmployeeRepository employeeRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final BusinessMetricsService businessMetricsService;
    private final AuditLogService auditLogService;

    @Transactional
    public PaymentDTO create(Long employeeId, LocalDate from, LocalDate to) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with id: " + employeeId));

        if (from == null || to == null || from.isAfter(to)) {
            throw new IllegalArgumentException("Invalid payment period.");
        }

        if (paymentRepository
                .existsByEmployeeIdAndFromDateLessThanEqualAndToDateGreaterThanEqual(
                        employeeId, to, from)) {
            throw new IllegalStateException(
                    "There is already a payment overlapping this period for the employee.");
        }

        // No liquidar sobre datos sucios: jornadas auto-cerradas sin corregir.
        boolean hasPendingFixes = timeEntryRepository.findByAutoClosedTrue().stream()
                .anyMatch(e -> e.getEmployee() != null
                        && e.getEmployee().getId().equals(employeeId)
                        && e.getClockIn() != null
                        && !e.getClockIn().toLocalDate().isBefore(from)
                        && !e.getClockIn().toLocalDate().isAfter(to));
        if (hasPendingFixes) {
            throw new IllegalStateException(
                    "The period has auto-closed time entries pending correction. "
                            + "Fix them before marking the period as paid.");
        }

        PayrollRowDTO row = businessMetricsService.getPayrollRow(employeeId, from, to);

        Payment payment = paymentRepository.save(Payment.builder()
                .employee(employee)
                .fromDate(from)
                .toDate(to)
                .workedMinutes(row.getWorkedMinutes())
                .doubleMinutes(row.getDoubleMinutes())
                .payableMinutes(row.getPayableMinutes())
                .hourlyRate(row.getHourlyRate())
                .amount(row.getAmount())
                .createdBy(currentUsername())
                .build());

        auditLogService.record("PAYMENT", payment.getId(), "CREATE",
                "employeeId=" + employeeId + ", period=" + from + ".." + to
                        + ", payableMinutes=" + row.getPayableMinutes()
                        + ", amount=" + row.getAmount());

        return toDTO(payment);
    }

    /** Reabre (borra) un pago: las jornadas del periodo vuelven a ser editables. */
    @Transactional
    public void delete(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found with id: " + paymentId));

        auditLogService.record("PAYMENT", paymentId, "DELETE",
                "reopened: employeeId=" + payment.getEmployee().getId()
                        + ", period=" + payment.getFromDate() + ".." + payment.getToDate()
                        + ", amount=" + payment.getAmount());

        paymentRepository.delete(payment);
    }

    public Page<PaymentDTO> getPage(int page, int size) {
        return paymentRepository.findAll(
                        PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                                Sort.by(Sort.Direction.DESC, "id")))
                .map(this::toDTO);
    }

    private PaymentDTO toDTO(Payment p) {
        return PaymentDTO.builder()
                .id(p.getId())
                .employeeId(p.getEmployee().getId())
                .employeeName(p.getEmployee().getName() + " " + p.getEmployee().getLastName())
                .from(p.getFromDate())
                .to(p.getToDate())
                .workedMinutes(p.getWorkedMinutes())
                .doubleMinutes(p.getDoubleMinutes())
                .payableMinutes(p.getPayableMinutes())
                .hourlyRate(p.getHourlyRate())
                .amount(p.getAmount())
                .createdBy(p.getCreatedBy())
                .createdAt(p.getCreatedAt())
                .build();
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }
}
