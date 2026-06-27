package com.tuusuario.employee_time_tracker.Model.Dto;

import com.tuusuario.employee_time_tracker.Model.Enums.WorkState;
import lombok.*;

/** Empleado tal como lo ve el kiosco: nombre, estado y si tiene PIN configurado. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KioskEmployeeDTO {
    private Long id;
    private String fullName;
    private WorkState state;
    private boolean hasPin;
}
