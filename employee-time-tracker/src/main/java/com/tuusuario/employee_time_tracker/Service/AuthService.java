package com.tuusuario.employee_time_tracker.Service;

import com.tuusuario.employee_time_tracker.Dto.AuthRequestDTO;
import com.tuusuario.employee_time_tracker.Dto.AuthResponseDTO;
import com.tuusuario.employee_time_tracker.Dto.RegisterRequestDTO;
import com.tuusuario.employee_time_tracker.Model.User;
import com.tuusuario.employee_time_tracker.Repository.UserRepository;
import com.tuusuario.employee_time_tracker.Util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public String register(RegisterRequestDTO request) {
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        userRepository.save(user);

        return "User registered successfully";
    }

    public AuthResponseDTO login(AuthRequestDTO request) {

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean passwordMatches =
                passwordEncoder.matches(request.getPassword(), user.getPassword());

        if (!passwordMatches) {
            throw new RuntimeException("Invalid username or password");
        }

        String token = jwtUtil.generateToken(user.getUsername());

        return new AuthResponseDTO(token);
    }
}
