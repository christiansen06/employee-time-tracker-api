package com.tuusuario.employee_time_tracker.Model.Dto;

import lombok.*;

import java.time.LocalDate;

/** Minutos trabajados (netos de breaks) en un dia puntual. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyHoursDTO {
    private LocalDate date;
    private long workedMinutes;
}
