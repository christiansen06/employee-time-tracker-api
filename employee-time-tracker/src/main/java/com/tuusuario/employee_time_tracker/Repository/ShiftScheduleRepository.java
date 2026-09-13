package com.tuusuario.employee_time_tracker.Repository;

import com.tuusuario.employee_time_tracker.Model.Entity.ShiftSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftScheduleRepository extends JpaRepository<ShiftSchedule, Long> {

    List<ShiftSchedule> findByShiftDateBetween(LocalDate from, LocalDate to);

    Optional<ShiftSchedule> findByEmployeeIdAndShiftDate(Long employeeId, LocalDate shiftDate);

    void deleteByShiftDateBetween(LocalDate from, LocalDate to);
}
