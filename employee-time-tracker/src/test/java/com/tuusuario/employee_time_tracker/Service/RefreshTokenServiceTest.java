package com.tuusuario.employee_time_tracker.Service;

import com.tuusuario.employee_time_tracker.Model.Entity.RefreshToken;
import com.tuusuario.employee_time_tracker.Model.Entity.User;
import com.tuusuario.employee_time_tracker.Repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks private RefreshTokenService service;

    private final User user = User.builder().id(1L).username("admin").build();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "refreshExpirationMs", 2592000000L);
    }

    @Test
    void issueStoresOnlyTheHash() {
        String raw = service.issue(user);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        assertThat(raw).isNotBlank();
        assertThat(captor.getValue().getTokenHash())
                .isNotEqualTo(raw)
                .hasSize(64); // SHA-256 en hex
        assertThat(captor.getValue().getExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(captor.getValue().getRevoked()).isFalse();
    }

    @Test
    void consumeFailsForUnknownToken() {
        when(refreshTokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consume("inexistente"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void consumeFailsForRevokedToken() {
        RefreshToken token = RefreshToken.builder().user(user)
                .expiresAt(LocalDateTime.now().plusDays(1)).revoked(true).build();
        when(refreshTokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.consume("revocado"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void consumeFailsForExpiredToken() {
        RefreshToken token = RefreshToken.builder().user(user)
                .expiresAt(LocalDateTime.now().minusMinutes(1)).revoked(false).build();
        when(refreshTokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.consume("vencido"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void consumeRevokesTokenAndReturnsOwner() {
        RefreshToken token = RefreshToken.builder().user(user)
                .expiresAt(LocalDateTime.now().plusDays(1)).revoked(false).build();
        when(refreshTokenRepository.findByTokenHash(anyString()))
                .thenReturn(Optional.of(token));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        User owner = service.consume("valido");

        assertThat(owner).isSameAs(user);
        assertThat(token.getRevoked()).isTrue();
    }

    @Test
    void consumeFailsForBlankToken() {
        assertThatThrownBy(() -> service.consume(" "))
                .isInstanceOf(BadCredentialsException.class);
    }
}
