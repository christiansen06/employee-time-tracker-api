package com.tuusuario.employee_time_tracker.Model.Dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/** La semana de un empleado: sus 7 dias y el total planificado. */
@Value
@Builder
public class ScheduleRowDTO {
    Long employeeId;
    String employeeName;
    /** Siempre 7 celdas, de lunes a domingo (null donde no hay nada cargado). */
    List<ScheduleCellDTO> cells;
    long plannedMinutes;
}
