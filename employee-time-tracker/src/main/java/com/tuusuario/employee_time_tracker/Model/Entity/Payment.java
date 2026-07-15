package com.tuusuario.employee_time_tracker.Model.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Pago realizado a un empleado por un periodo. Actua como cierre:
 * las jornadas dentro del periodo quedan congeladas (no se editan,
 * borran ni marcan dobles) hasta que el pago se reabra (borre).
 */
@Entity
@Table(name = "payments")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    @Column(name = "worked_minutes", nullable = false)
    private Long workedMinutes;

    @Column(name = "double_minutes", nullable = false)
    private Long doubleMinutes;

    @Column(name = "payable_minutes", nullable = false)
    private Long payableMinutes;

    /** Tarifa al momento del pago (snapshot: cambios futuros no lo alteran). */
    @Column(name = "hourly_rate", precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_by")
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
