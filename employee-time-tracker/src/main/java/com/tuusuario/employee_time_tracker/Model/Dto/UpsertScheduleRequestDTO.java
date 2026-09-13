package com.tuusuario.employee_time_tracker.Model.Dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/** Alta o edicion de una celda del horario. */
@Data
public class UpsertScheduleRequestDTO {

    @NotNull(message = "employeeId is required")
    private Long employeeId;

    @NotNull(message = "date is required")
    private LocalDate date;

    private LocalTime startTime;
    private LocalTime endTime;
    private boolean dayOff;

    @Size(max = 60, message = "note must be at most 60 characters")
    private String note;
}
