package com.tuusuario.employee_time_tracker.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Habilita el llenado automatico de created_at / updated_at. */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
