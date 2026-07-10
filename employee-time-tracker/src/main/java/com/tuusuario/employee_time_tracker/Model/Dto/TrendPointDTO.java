package com.tuusuario.employee_time_tracker.Model.Dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

/** Punto de la serie temporal de horas trabajadas (para graficos). */
@Value
@Builder
public class TrendPointDTO {
    LocalDate date;
    long workedMinutes;
    double workedHours;
    int employeesWorked;
}
