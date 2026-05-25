package com.tuusuario.employee_time_tracker.Model.Dto;

import lombok.*;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WorkedHoursResponseDTO {
    private Long employeeId;
    private String employeeName;
    private Long totalWorkedMinutes;
    private Double totalWorkedHours;
    private Long overtimeMinutes;
    private Double overtimeHours;
}
