package com.tuusuario.employee_time_tracker.Model.Dto;

import com.tuusuario.employee_time_tracker.Model.Enums.WorkState;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Estado de fichaje actual del empleado autenticado.
 * El frontend usa {@code state} para habilitar/deshabilitar los botones.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentStatusDTO {

    private Long employeeId;
    private String employeeName;

    private WorkState state;

    /** Jornada abierta (null si no hay). */
    private Long timeEntryId;
    private LocalDateTime clockIn;

    /** Break en curso (null si no esta en break). */
    private Long activeBreakId;
    private LocalDateTime breakStart;
}
