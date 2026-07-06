package com.tuusuario.employee_time_tracker.Model.Dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

/** Desglose dia a dia de la semana actual, estilo tarjeta de fichaje. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyHoursDetailDTO {
    private Long employeeId;
    private String employeeName;
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private List<DailyHoursDTO> days;
    private long totalWorkedMinutes;
    private double totalWorkedHours;
}
