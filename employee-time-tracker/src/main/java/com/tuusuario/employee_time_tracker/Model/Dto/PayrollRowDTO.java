package com.tuusuario.employee_time_tracker.Model.Dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/** Liquidacion de un empleado en el periodo: horas a pagar y monto. */
@Value
@Builder
public class PayrollRowDTO {
    Long employeeId;
    String employeeName;
    /** Minutos netos realmente trabajados (breaks descontados). */
    long workedMinutes;
    /** Minutos de jornadas marcadas como dobles (feriados). */
    long doubleMinutes;
    /** Minutos a pagar: trabajados + los dobles una vez mas. */
    long payableMinutes;
    double workedHours;
    double doubleHours;
    double payableHours;
    /** Null si el empleado no tiene valor hora cargado. */
    BigDecimal hourlyRate;
    /** payableMinutes/60 x hourlyRate. Null si no hay valor hora. */
    BigDecimal amount;
    /** Pago exacto de este periodo, si ya se realizo (sino null). */
    Long paymentId;
    java.time.LocalDateTime paidAt;
}
