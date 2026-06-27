package com.tuusuario.employee_time_tracker.Repository;

import com.tuusuario.employee_time_tracker.Model.Entity.TimeEntry;
import com.tuusuario.employee_time_tracker.Model.Enums.TimeEntryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {
    List<TimeEntry> findByStatus(TimeEntryStatus status);
    List<TimeEntry> findByClockInBetween(LocalDateTime start, LocalDateTime end);
    List<TimeEntry> findByEmployeeId(Long employeeId);

    boolean existsByEmployeeIdAndStatusIn(Long employeeId, Collection<TimeEntryStatus> statuses);

    Optional<TimeEntry> findFirstByEmployeeIdAndStatusInOrderByClockInDesc(
            Long employeeId, Collection<TimeEntryStatus> statuses);

    Optional<TimeEntry> findFirstByEmployeeIdAndStatusOrderByClockInDesc(
            Long employeeId, TimeEntryStatus status);
}
