package com.tuusuario.employee_time_tracker.Model.Dto;

import com.tuusuario.employee_time_tracker.Model.Enums.BreakStatus;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreakResponseDTO {
    private Long id;
    private LocalDateTime breakStart;
    private LocalDateTime breakEnd;
    private Long durationMinutes;
    private BreakStatus breakStatus;
}
