package com.tuusuario.employee_time_tracker.Model.Dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalTime;

/** Un turno planificado (una celda de la grilla de horarios). */
@Value
@Builder
public class ScheduleCellDTO {
    Long id;
    LocalDate date;
    LocalTime startTime;
    LocalTime endTime;
    boolean dayOff;
    String note;
    /** Minutos planificados: 0 si es franco o si solo hay una nota. */
    long plannedMinutes;
    /** Texto ya armado para mostrar y para la imagen: "9 a 19", "Franco", "Mossa". */
    String label;
}
