package com.tuusuario.employee_time_tracker.Model.Entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.tuusuario.employee_time_tracker.Model.Enums.TimeEntryStatus;
import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonBackReference;
import java.time.LocalDateTime;
import java.util.List;

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

    @Enumerated(EnumType.STRING)
    private TimeEntryStatus status;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    @JsonBackReference
    private Employee employee;

    @OneToMany(mappedBy = "timeEntry")
    @JsonManagedReference
    private List<BreakEntry> breaks;
}
