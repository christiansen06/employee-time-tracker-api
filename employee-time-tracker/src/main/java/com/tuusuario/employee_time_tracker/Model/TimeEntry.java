package com.tuusuario.employee_time_tracker.Model;

import com.tuusuario.employee_time_tracker.Enums.TimeEntryStatus;
import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonBackReference;
import java.time.LocalDateTime;

@Entity
@Table(name = "time_entries")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime clockIn;

    private LocalDateTime clockOut;

    private LocalDateTime breakStart;

    private LocalDateTime breakEnd;

    @Enumerated(EnumType.STRING)
    private TimeEntryStatus status;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    @JsonBackReference
    private Employee employee;
}
