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

}
