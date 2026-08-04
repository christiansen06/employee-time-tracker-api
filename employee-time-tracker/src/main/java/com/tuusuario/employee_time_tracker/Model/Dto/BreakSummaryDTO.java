package com.tuusuario.employee_time_tracker.Model.Dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreakSummaryDTO {
    private Long breakId;
    private Long employeeId;
    private String employeeName;
    private LocalDateTime breakStart;

    /** Minutos que lleva el break en curso (para verlo en "Ahora"). */
    private Long minutesElapsed;

    /**
     * true si ya paso el umbral configurado. No es un error (puede ser un
     * break largo legitimo): solo se resalta para que el admin lo mire.
     */
    private Boolean longBreak;
}
