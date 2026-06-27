package com.tuusuario.employee_time_tracker.Model.Entity;

import com.tuusuario.employee_time_tracker.Model.Enums.Role;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    // VARCHAR en vez de ENUM de MySQL: permite agregar roles nuevos (ej. KIOSK)
    // sin tener que recrear la columna.
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "varchar(20)")
    private Role role;

    /**
     * Empleado vinculado a esta cuenta de login.
     * Para usuarios EMPLOYEE apunta a su ficha; para ADMIN puede ser null.
     */
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "employee_id", unique = true)
    private Employee employee;
}
