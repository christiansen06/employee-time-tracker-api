package com.tuusuario.employee_time_tracker.Model.Dto;

import lombok.*;

/** Un tramo trabajado dentro de un dia: de que hora a que hora, y cuanto break tuvo. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkIntervalDTO {
    /** Hora de entrada, formato HH:mm. */
    private String clockIn;
    /** Hora de salida, formato HH:mm. */
    private String clockOut;
    private long breakMinutes;
}
