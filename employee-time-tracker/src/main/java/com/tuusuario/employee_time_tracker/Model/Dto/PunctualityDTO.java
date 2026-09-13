package com.tuusuario.employee_time_tracker.Model.Dto;

import lombok.Builder;
import lombok.Value;

/**
 * Puntualidad de un empleado vs su hora esperada de entrada. La hora
 * esperada sale, dia por dia, del horario planificado (shift_schedules) o,
 * si ese dia no tiene uno cargado, del horario fijo de la ficha.
 */
@Value
@Builder
public class PunctualityDTO {
    Long employeeId;
    String employeeName;
    /** "9:00" si usa un horario fijo, o "Según horario cargado" si varía por día. */
    String referenceLabel;
    int daysEvaluated;
    int lateArrivals;
    double latePercentage;
    /** Promedio de minutos de retraso, solo sobre los dias que llego tarde. */
    double avgLateMinutes;
}
