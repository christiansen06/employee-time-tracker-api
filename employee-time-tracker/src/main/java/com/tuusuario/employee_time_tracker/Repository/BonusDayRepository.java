package com.tuusuario.employee_time_tracker.Repository;

import com.tuusuario.employee_time_tracker.Model.Entity.BonusDay;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BonusDayRepository extends JpaRepository<BonusDay, Long> {

    @EntityGraph(attributePaths = "employees")
    List<BonusDay> findByBonusDateBetweenOrderByBonusDateAsc(LocalDate from, LocalDate to);

    boolean existsByBonusDate(LocalDate date);

    boolean existsByBonusDateAndIdNot(LocalDate date, Long id);
}
