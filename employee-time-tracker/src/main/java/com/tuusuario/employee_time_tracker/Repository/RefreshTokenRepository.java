package com.tuusuario.employee_time_tracker.Repository;

import com.tuusuario.employee_time_tracker.Model.Entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    void deleteByUser_IdAndExpiresAtBefore(Long userId, LocalDateTime cutoff);
}
