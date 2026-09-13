package com.tuusuario.employee_time_tracker.Model.Dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;

/** Grilla completa de la semana: un renglon por empleado activo. */
@Value
@Builder
public class ScheduleWeekDTO {
    LocalDate from;
    LocalDate to;
    List<LocalDate> days;
    List<ScheduleRowDTO> rows;
}
