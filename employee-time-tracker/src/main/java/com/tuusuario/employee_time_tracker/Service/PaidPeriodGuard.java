package com.tuusuario.employee_time_tracker.Service;

import com.tuusuario.employee_time_tracker.Repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Regla unica de cierre por pago: una vez liquidado un periodo, nada de lo
 * que afecte las horas de ese empleado (jornadas y breaks) puede cambiar
 * hasta reabrir el pago.
 */
@Component
@RequiredArgsConstructor
public class PaidPeriodGuard {

    private final PaymentRepository paymentRepository;

    public void assertNotPaid(Long employeeId, LocalDate date) {
        if (employeeId == null || date == null) {
            return;
        }
        if (paymentRepository
                .existsByEmployeeIdAndFromDateLessThanEqualAndToDateGreaterThanEqual(
                        employeeId, date, date)) {
            throw new IllegalStateException(
                    "This period was already paid and is locked. "
                            + "Reopen the payment first (Pagos realizados).");
        }
    }
}
