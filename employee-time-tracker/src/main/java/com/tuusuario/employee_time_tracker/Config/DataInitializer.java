package com.tuusuario.employee_time_tracker.Config;

import com.tuusuario.employee_time_tracker.Model.Entity.User;
import com.tuusuario.employee_time_tracker.Model.Enums.Role;
import com.tuusuario.employee_time_tracker.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea un usuario ADMIN inicial al arrancar la aplicacion si todavia
 * no existe ningun ADMIN. Las credenciales se leen de variables de entorno
 * (ADMIN_USERNAME / ADMIN_PASSWORD), con defaults solo para desarrollo local.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:admin1234}")
    private String adminPassword;

    @Value("${app.kiosk.username:kiosk}")
    private String kioskUsername;

    @Value("${app.kiosk.password:kiosk1234}")
    private String kioskPassword;

    @Override
    public void run(String... args) {
        seedUser(Role.ADMIN, adminUsername, adminPassword);
        seedUser(Role.KIOSK, kioskUsername, kioskPassword);
    }

    /** Crea el usuario del rol indicado si todavia no existe ninguno con ese rol. */
    private void seedUser(Role role, String username, String rawPassword) {

        if (userRepository.existsByRole(role)) {
            log.info("Ya existe un usuario {}. No se crea el inicial.", role);
            return;
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .build();

        userRepository.save(user);

        log.warn("Usuario {} inicial creado con username '{}'. " +
                "Cambia la contraseña por defecto si estas en produccion.", role, username);
    }
}
