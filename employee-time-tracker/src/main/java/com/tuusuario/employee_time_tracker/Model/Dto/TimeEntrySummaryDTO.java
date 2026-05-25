package com.tuusuario.employee_time_tracker.Model.Dto;

import com.tuusuario.employee_time_tracker.Model.Enums.TimeEntryStatus;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeEntrySummaryDTO {
    private Long timeEntryId;
    private Long employeeId;
    private String employeeName;
    private LocalDateTime clockIn;
    private LocalDateTime clockOut;
    private TimeEntryStatus status;
}
