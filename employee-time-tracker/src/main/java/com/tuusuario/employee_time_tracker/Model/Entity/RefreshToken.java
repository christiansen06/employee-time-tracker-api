package com.tuusuario.employee_time_tracker.Model.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Token de refresco opaco. Nunca se persiste el valor en claro:
 * solo su hash SHA-256, para que un vuelco de la base no sirva
 * para robar sesiones.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private Boolean revoked;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
