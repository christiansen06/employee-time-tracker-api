package com.tuusuario.employee_time_tracker.Repository;

import com.tuusuario.employee_time_tracker.Model.Entity.TimeEntry;
import com.tuusuario.employee_time_tracker.Model.Enums.TimeEntryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {
    List<TimeEntry> findByClockInBetween(LocalDateTime start, LocalDateTime end);
    Page<TimeEntry> findByClockInBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
    List<TimeEntry> findByEmployeeId(Long employeeId);
    Page<TimeEntry> findByEmployeeId(Long employeeId, Pageable pageable);
    Page<TimeEntry> findByEmployeeIdAndClockInBetween(
            Long employeeId, LocalDateTime start, LocalDateTime end, Pageable pageable);

    /** Jornadas cerradas del empleado que se pisan con el rango [start, end). */
    boolean existsByEmployeeIdAndClockInLessThanAndClockOutGreaterThan(
            Long employeeId, LocalDateTime end, LocalDateTime start);

    boolean existsByEmployeeId(Long employeeId);

    boolean existsByEmployeeIdAndStatusIn(Long employeeId, Collection<TimeEntryStatus> statuses);

    Optional<TimeEntry> findFirstByEmployeeIdAndStatusInOrderByClockInDesc(
            Long employeeId, Collection<TimeEntryStatus> statuses);

    Optional<TimeEntry> findFirstByEmployeeIdAndStatusOrderByClockInDesc(
            Long employeeId, TimeEntryStatus status);

    List<TimeEntry> findByStatusInAndClockInBefore(
            Collection<TimeEntryStatus> statuses, LocalDateTime before);

    List<TimeEntry> findByStatusIn(Collection<TimeEntryStatus> statuses);
}
