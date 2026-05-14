package com.tuusuario.employee_time_tracker.Repository;

import com.tuusuario.employee_time_tracker.Model.TimeEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {
}
