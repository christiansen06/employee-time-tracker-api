package com.tuusuario.employee_time_tracker.Model.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Turno PLANIFICADO de un empleado para un dia (lo que el admin arma por
 * adelantado), distinto de {@link TimeEntry}, que es lo realmente fichado.
 *
 * Un solo turno por empleado y dia, igual que la planilla que se usaba antes.
 */
@Entity
@Table(name = "shift_schedules",
       uniqueConstraints = @UniqueConstraint(
               name = "uq_shift_schedules_employee_date",
               columnNames = {"employee_id", "shift_date"}))
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "shift_date", nullable = false)
    private LocalDate shiftDate;

    /** Null cuando es franco o cuando solo hay una nota. */
    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    /** Dia libre: se muestra como "Franco" y no suma horas. */
    @Column(name = "day_off", nullable = false)
    private Boolean dayOff;

    /** Aclaracion libre para casos que no son un horario (ej.: "Mossa"). */
    @Column(name = "note", length = 60)
    private String note;

    @Column(name = "created_by")
    private String createdBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
