package com.tuusuario.employee_time_tracker.Model.Dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Liquidacion del periodo: cuanto pagarle a cada empleado y el total. */
@Value
@Builder
public class PayrollReportDTO {
    LocalDate from;
    LocalDate to;
    List<PayrollRowDTO> rows;
    long totalPayableMinutes;
    /** Suma de los montos de los empleados con valor hora cargado. */
    BigDecimal totalAmount;
    /** Empleados del reporte sin valor hora (su monto no entra en el total). */
    int employeesWithoutRate;
}
