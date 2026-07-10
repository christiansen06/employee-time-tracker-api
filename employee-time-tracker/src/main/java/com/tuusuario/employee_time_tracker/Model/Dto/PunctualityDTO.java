package com.tuusuario.employee_time_tracker.Model.Dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalTime;

/** Puntualidad de un empleado vs su hora esperada de entrada. */
@Value
@Builder
public class PunctualityDTO {
    Long employeeId;
    String employeeName;
    LocalTime expectedClockIn;
    int daysEvaluated;
    int lateArrivals;
    double latePercentage;
    /** Promedio de minutos de retraso, solo sobre los dias que llego tarde. */
    double avgLateMinutes;
}
