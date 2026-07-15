package com.tuusuario.employee_time_tracker.Repository;

import com.tuusuario.employee_time_tracker.Model.Entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Existe un pago del empleado que se solape con [from, to].
     * Para saber si UNA fecha cae en un periodo pagado, pasar from = to = fecha.
     */
    boolean existsByEmployeeIdAndFromDateLessThanEqualAndToDateGreaterThanEqual(
            Long employeeId, LocalDate to, LocalDate from);

    /** Pago exacto del periodo (para mostrar PAGADO en la liquidacion). */
    Optional<Payment> findFirstByEmployeeIdAndFromDateAndToDate(
            Long employeeId, LocalDate fromDate, LocalDate toDate);
}
