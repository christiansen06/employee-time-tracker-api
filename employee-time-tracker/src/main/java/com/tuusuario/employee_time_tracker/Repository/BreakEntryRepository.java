package com.tuusuario.employee_time_tracker.Repository;

import com.tuusuario.employee_time_tracker.Model.Entity.BreakEntry;
import com.tuusuario.employee_time_tracker.Model.Enums.BreakStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BreakEntryRepository extends JpaRepository<BreakEntry, Long> {
    List<BreakEntry> findByBreakStatus(BreakStatus breakStatus);

    Optional<BreakEntry> findFirstByTimeEntry_Employee_IdAndBreakStatus(
            Long employeeId, BreakStatus breakStatus);
}
