package com.tuusuario.employee_time_tracker.Service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalTime;

/**
 * Se hace ping a si mismo cada 10 minutos (en horario laboral) para que
 * Render free no duerma la instancia y Neon no suspenda la base.
 *
 * Es un respaldo del keep-alive externo (cron-job.org), que demostro ser
 * un punto unico de falla: si se deshabilita, la app vuelve a dormirse.
 * Con esto, una vez despierta (primer uso del dia o ping externo), la app
 * se mantiene despierta sola hasta el fin de la ventana horaria.
 *
 * La URL se toma de app.self-ping-url o de RENDER_EXTERNAL_URL (variable
 * que Render define automaticamente). Sin URL (desarrollo local) no hace nada.
 */
@Component
public class SelfPingJob {

    private static final Logger log = LoggerFactory.getLogger(SelfPingJob.class);

    @Value("${app.self-ping-url:${RENDER_EXTERNAL_URL:}}")
    private String baseUrl;

    @Value("${app.self-ping-from-hour:6}")
    private int fromHour;

    @Value("${app.self-ping-to-hour:21}")
    private int toHour;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    @PostConstruct
    void logConfig() {
        if (baseUrl == null || baseUrl.isBlank()) {
            log.info("Self-ping desactivado (sin app.self-ping-url ni RENDER_EXTERNAL_URL).");
        } else {
            log.info("Self-ping activo hacia {} entre las {} y las {} hs.", baseUrl, fromHour, toHour);
        }
    }

    @Scheduled(fixedDelay = 600_000, initialDelay = 600_000)
    public void ping() {
        if (baseUrl == null || baseUrl.isBlank()) {
            return;
        }

        int hour = LocalTime.now().getHour();
        if (hour < fromHour || hour >= toHour) {
            return; // fuera de la ventana laboral: dejar dormir para no gastar horas gratis
        }

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/health"))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();
            client.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            log.warn("Self-ping fallo: {}", e.getMessage());
        }
    }
}
