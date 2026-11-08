package com.tuusuario.employee_time_tracker.Model.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Dia con bono: cada hora neta trabajada ese dia por los empleados elegidos
 * suma {@code amountPerHour} a su liquidacion. Es fijo: no se duplica
 * aunque la jornada sea feriado (x2).
 */
@Entity
@Table(name = "bonus_days")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BonusDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bonus_date", nullable = false, unique = true)
    private LocalDate bonusDate;

    @Column(name = "amount_per_hour", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountPerHour;

    /** Motivo libre (ej.: "Food truck + local"). */
    @Column(name = "note", length = 120)
    private String note;

    @ManyToMany
    @JoinTable(name = "bonus_day_employees",
            joinColumns = @JoinColumn(name = "bonus_day_id"),
            inverseJoinColumns = @JoinColumn(name = "employee_id"))
    @Builder.Default
    private Set<Employee> employees = new HashSet<>();

    @Column(name = "created_by")
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean includes(Long employeeId) {
        return employees.stream().anyMatch(e -> e.getId().equals(employeeId));
    }
}
