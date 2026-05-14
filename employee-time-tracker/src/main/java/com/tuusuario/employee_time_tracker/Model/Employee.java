package com.tuusuario.employee_time_tracker.Model;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import java.util.List;

@Entity
@Table(name = "employees")
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

    @OneToMany(mappedBy = "employee")
    @JsonManagedReference
    private List<TimeEntry> timeEntries;
}
