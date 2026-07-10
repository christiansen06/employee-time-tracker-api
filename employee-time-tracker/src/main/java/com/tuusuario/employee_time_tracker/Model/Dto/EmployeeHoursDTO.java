package com.tuusuario.employee_time_tracker.Model.Dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/** Horas y costo estimado de un empleado dentro de un rango de fechas. */
@Value
@Builder
public class EmployeeHoursDTO {
    Long employeeId;
    String employeeName;
    long workedMinutes;
    double workedHours;
    long breakMinutes;
    int daysWorked;
    /** Null si el empleado no tiene valor hora cargado. */
    BigDecimal estimatedCost;
}
