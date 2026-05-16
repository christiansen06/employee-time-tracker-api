package com.tuusuario.employee_time_tracker.Dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EmployeeResponseDTO {
    Long id;
    String name;
    String lastName;
    String email;
    String position;
    Boolean active;
}
