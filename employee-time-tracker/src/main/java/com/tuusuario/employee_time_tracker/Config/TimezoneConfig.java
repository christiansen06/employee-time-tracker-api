package com.tuusuario.employee_time_tracker.Config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * Fija la zona horaria de la JVM. Critico en la nube: el servidor de Render
 * corre en UTC y sin esto las fichadas quedarian 3 horas adelantadas.
 */
@Configuration
public class TimezoneConfig {

    private static final Logger log = LoggerFactory.getLogger(TimezoneConfig.class);

    @Value("${app.timezone:America/Argentina/Buenos_Aires}")
    private String timezone;

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone(timezone));
        log.info("Zona horaria de la aplicacion: {}", timezone);
    }
}
