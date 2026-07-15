package com.tuusuario.employee_time_tracker.Model.Dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Pago realizado (historial / recibo simple). */
@Value
@Builder
public class PaymentDTO {
    Long id;
    Long employeeId;
    String employeeName;
    LocalDate from;
    LocalDate to;
    long workedMinutes;
    long doubleMinutes;
    long payableMinutes;
    BigDecimal hourlyRate;
    BigDecimal amount;
    String createdBy;
    LocalDateTime createdAt;
}
