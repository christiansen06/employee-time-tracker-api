package com.tuusuario.employee_time_tracker.Model.Dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;

/**
 * Ausencias estimadas: dias en los que el local estuvo operativo
 * (algun empleado ficho) pero este empleado no registro jornada.
 */
@Value
@Builder
public class AbsenceDTO {
    Long employeeId;
    String employeeName;
    int businessOpenDays;
    int daysWorked;
    int daysAbsent;
    double absencePercentage;
    List<LocalDate> absentDates;
}
