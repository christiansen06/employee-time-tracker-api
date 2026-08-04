package com.tuusuario.employee_time_tracker.Model.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/** Alta manual de un break por parte del ADMIN. */
@Data
public class CreateBreakRequestDTO {

    @NotNull(message = "timeEntryId is required")
    private Long timeEntryId;

    @NotNull(message = "breakStart is required")
    private LocalDateTime breakStart;

    @NotNull(message = "breakEnd is required")
    private LocalDateTime breakEnd;
}
