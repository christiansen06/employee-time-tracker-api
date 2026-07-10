package com.tuusuario.employee_time_tracker.Model.Dto;

import lombok.Builder;
import lombok.Value;

/** Horas extra de un empleado en el rango: por exceso diario y semanal. */
@Value
@Builder
public class OvertimeDTO {
    Long employeeId;
    String employeeName;
    /** Minutos por encima del umbral diario, sumados dia a dia. */
    long dailyOvertimeMinutes;
    /** Minutos por encima del tope semanal, sumados semana a semana. */
    long weeklyOvertimeMinutes;
    int weeklyHoursTarget;
    double dailyOvertimeHours;
    double weeklyOvertimeHours;
}
