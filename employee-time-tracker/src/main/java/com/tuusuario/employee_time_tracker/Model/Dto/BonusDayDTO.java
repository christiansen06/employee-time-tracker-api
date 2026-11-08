package com.tuusuario.employee_time_tracker.Model.Dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Dia con bono y quienes lo cobran. */
@Value
@Builder
public class BonusDayDTO {
    Long id;
    LocalDate date;
    BigDecimal amountPerHour;
    String note;
    List<Long> employeeIds;
    List<String> employeeNames;
}
