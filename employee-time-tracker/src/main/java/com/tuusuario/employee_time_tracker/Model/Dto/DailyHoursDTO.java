package com.tuusuario.employee_time_tracker.Model.Dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

/** Minutos trabajados (netos de breaks) en un dia puntual, con sus tramos. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyHoursDTO {
    private LocalDate date;
    private long workedMinutes;
    /** Tramos entrada-salida del dia (puede haber mas de uno). */
    private List<WorkIntervalDTO> intervals;
}
