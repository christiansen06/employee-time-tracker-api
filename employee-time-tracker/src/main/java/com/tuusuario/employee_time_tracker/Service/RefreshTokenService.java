package com.tuusuario.employee_time_tracker.Service;

import com.tuusuario.employee_time_tracker.Model.Entity.RefreshToken;
import com.tuusuario.employee_time_tracker.Model.Entity.User;
import com.tuusuario.employee_time_tracker.Repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration-ms:2592000000}")
    private long refreshExpirationMs;

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Emite un refresh token nuevo y devuelve su valor en claro (una unica vez). */
    @Transactional
    public String issue(User user) {
        // Limpieza oportunista: los tokens vencidos del usuario ya no sirven.
        refreshTokenRepository.deleteByUser_IdAndExpiresAtBefore(
                user.getId(), LocalDateTime.now());

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        refreshTokenRepository.save(RefreshToken.builder()
                .tokenHash(sha256(raw))
                .user(user)
                .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(refreshExpirationMs)))
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .build());

        return raw;
    }

    /**
     * Valida el token y lo consume (rotacion: cada refresh token sirve una sola vez).
     * Devuelve el usuario dueño para emitir credenciales nuevas.
     */
    @Transactional
    public User consume(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        RefreshToken token = refreshTokenRepository.findByTokenHash(sha256(rawToken))
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (Boolean.TRUE.equals(token.getRevoked())
                || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        token.setRevoked(true);
        refreshTokenRepository.save(token);

        return token.getUser();
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
