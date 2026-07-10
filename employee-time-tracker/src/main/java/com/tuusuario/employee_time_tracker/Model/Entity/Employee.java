package com.tuusuario.employee_time_tracker.Model.Entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "employees")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String position;

    @Column(nullable = false)
    private Boolean active;

    /** PIN de fichaje en el kiosco, hasheado con BCrypt. Null hasta que el admin lo asigna. */
    @Column(name = "pin_hash")
    private String pinHash;

    /** Valor hora para estimar costo laboral. Null = sin definir (queda fuera del calculo). */
    @Column(name = "hourly_rate", precision = 10, scale = 2)
    private java.math.BigDecimal hourlyRate;

    /** Hora esperada de entrada para medir puntualidad. Null = no se mide. */
    @Column(name = "expected_clock_in")
    private java.time.LocalTime expectedClockIn;

    /** Tope semanal de horas antes de contar overtime. Null = usa el default del negocio. */
    @Column(name = "weekly_hours_target")
    private Integer weeklyHoursTarget;

    @OneToMany(mappedBy = "employee")
    @JsonManagedReference
    private List<TimeEntry> timeEntries;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
