package com.tuusuario.employee_time_tracker.Model.Entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.tuusuario.employee_time_tracker.Model.Enums.BreakStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "break_entries")
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
@Builder
public class BreakEntry
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "break_start")
    private LocalDateTime breakStart;

    @Column(name = "break_end")
    private LocalDateTime breakEnd;

    @Enumerated(EnumType.STRING)
    private BreakStatus  breakStatus;

    @Column(name = "duration")
    private Long durationMinutes;

    @ManyToOne
    @JoinColumn(name = "time_entry_id")
    @JsonBackReference
    private TimeEntry timeEntry;
}
