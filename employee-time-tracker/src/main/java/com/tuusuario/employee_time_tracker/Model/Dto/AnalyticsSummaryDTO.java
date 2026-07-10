package com.tuusuario.employee_time_tracker.Model.Dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Resumen ejecutivo del periodo: horas, breaks, headcount y costo estimado. */
@Value
@Builder
public class AnalyticsSummaryDTO {
    LocalDate from;
    LocalDate to;
    int activeEmployees;
    long totalWorkedMinutes;
    double totalWorkedHours;
    long totalBreakMinutes;
    double avgHoursPerEmployeePerDay;
    /** Suma de horas x valor hora de los empleados con tarifa cargada. */
    BigDecimal estimatedLaborCost;
    /** Cuantos empleados tienen valor hora cargado (para saber que tan completo es el costo). */
    int employeesWithHourlyRate;
    List<EmployeeHoursDTO> perEmployee;
}
