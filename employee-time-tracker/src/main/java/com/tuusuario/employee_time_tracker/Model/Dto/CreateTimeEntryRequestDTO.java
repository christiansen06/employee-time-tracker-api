package com.tuusuario.employee_time_tracker.Model.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Alta manual de una jornada por parte del ADMIN
 * (ej.: el empleado trabajo pero se olvido de fichar).
 */
@Data
public class CreateTimeEntryRequestDTO {

    @NotNull(message = "employeeId is required")
    private Long employeeId;

    @NotNull(message = "clockIn is required")
    private LocalDateTime clockIn;

    @NotNull(message = "clockOut is required")
    private LocalDateTime clockOut;
}
