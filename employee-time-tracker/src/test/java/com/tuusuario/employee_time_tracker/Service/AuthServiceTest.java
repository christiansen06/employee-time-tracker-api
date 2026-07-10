package com.tuusuario.employee_time_tracker.Service;

import com.tuusuario.employee_time_tracker.Exception.DuplicateResourceException;
import com.tuusuario.employee_time_tracker.Model.Dto.AuthRequestDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.AuthResponseDTO;
import com.tuusuario.employee_time_tracker.Model.Dto.RegisterRequestDTO;
import com.tuusuario.employee_time_tracker.Model.Entity.User;
import com.tuusuario.employee_time_tracker.Model.Enums.Role;
import com.tuusuario.employee_time_tracker.Repository.EmployeeRepository;
import com.tuusuario.employee_time_tracker.Repository.UserRepository;
import com.tuusuario.employee_time_tracker.Util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private RefreshTokenService refreshTokenService;

    @InjectMocks private AuthService service;

    private AuthRequestDTO loginRequest(String username, String password) {
        AuthRequestDTO dto = new AuthRequestDTO();
        dto.setUsername(username);
        dto.setPassword(password);
        return dto;
    }

    @Test
    void loginFailsForUnknownUser() {
        when(userRepository.findByUsername("nadie")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(loginRequest("nadie", "x")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void loginFailsForWrongPassword() {
        User user = User.builder().username("admin").password("$hash$")
                .role(Role.ADMIN).build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("mala", "$hash$")).thenReturn(false);

        assertThatThrownBy(() -> service.login(loginRequest("admin", "mala")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void loginReturnsAccessAndRefreshTokens() {
        User user = User.builder().username("admin").password("$hash$")
                .role(Role.ADMIN).build();
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("buena", "$hash$")).thenReturn(true);
        when(jwtUtil.getDefaultExpirationMs()).thenReturn(3600000L);
        when(jwtUtil.generateToken("admin", 3600000L)).thenReturn("access-jwt");
        when(refreshTokenService.issue(user)).thenReturn("refresh-opaco");

        AuthResponseDTO res = service.login(loginRequest("admin", "buena"));

        assertThat(res.getToken()).isEqualTo("access-jwt");
        assertThat(res.getRefreshToken()).isEqualTo("refresh-opaco");
    }

    @Test
    void refreshIssuesNewTokensFromValidRefreshToken() {
        User user = User.builder().username("admin").password("$hash$")
                .role(Role.ADMIN).build();
        when(refreshTokenService.consume("viejo")).thenReturn(user);
        when(jwtUtil.getDefaultExpirationMs()).thenReturn(3600000L);
        when(jwtUtil.generateToken(anyString(), anyLong())).thenReturn("access-nuevo");
        when(refreshTokenService.issue(user)).thenReturn("refresh-nuevo");

        AuthResponseDTO res = service.refresh("viejo");

        assertThat(res.getToken()).isEqualTo("access-nuevo");
        assertThat(res.getRefreshToken()).isEqualTo("refresh-nuevo");
    }

    @Test
    void registerFailsForDuplicateUsername() {
        RegisterRequestDTO dto = new RegisterRequestDTO();
        dto.setUsername("admin");
        dto.setPassword("x");
        dto.setRole(Role.ADMIN);
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        assertThatThrownBy(() -> service.register(dto))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void registerEmployeeRequiresEmployeeId() {
        RegisterRequestDTO dto = new RegisterRequestDTO();
        dto.setUsername("empleado");
        dto.setPassword("x");
        dto.setRole(Role.EMPLOYEE);
        when(userRepository.existsByUsername("empleado")).thenReturn(false);

        assertThatThrownBy(() -> service.register(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("employeeId");
    }
}
