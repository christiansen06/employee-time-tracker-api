package com.tuusuario.employee_time_tracker.Model.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/** Correccion manual de una jornada por parte del ADMIN. */
@Data
public class UpdateTimeEntryRequestDTO {

    @NotNull(message = "clockIn is required")
    private LocalDateTime clockIn;

    @NotNull(message = "clockOut is required")
    private LocalDateTime clockOut;
}
