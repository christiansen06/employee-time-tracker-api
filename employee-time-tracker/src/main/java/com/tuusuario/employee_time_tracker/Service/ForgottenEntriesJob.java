package com.tuusuario.employee_time_tracker.Service;

import com.tuusuario.employee_time_tracker.Model.Entity.BreakEntry;
import com.tuusuario.employee_time_tracker.Model.Entity.TimeEntry;
import com.tuusuario.employee_time_tracker.Model.Enums.BreakStatus;
import com.tuusuario.employee_time_tracker.Model.Enums.TimeEntryStatus;
import com.tuusuario.employee_time_tracker.Repository.BreakEntryRepository;
import com.tuusuario.employee_time_tracker.Repository.TimeEntryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Cierra automaticamente las jornadas que quedaron abiertas de dias
 * anteriores (empleado que olvido fichar la salida).
 *
 * La jornada se cierra con salida = entrada (0 horas) y se marca
 * autoClosed=true para que el admin la corrija con el horario real
 * desde el dashboard. Asi nunca se pagan horas no verificadas.
 *
 * Corre a las 07:15 (dentro de la ventana del keep-alive, cuando la
 * app esta despierta) y tambien en cada arranque, por si el servidor
 * estaba dormido a esa hora.
 */
@Component
@RequiredArgsConstructor
public class ForgottenEntriesJob {

    private static final Logger log = LoggerFactory.getLogger(ForgottenEntriesJob.class);

    private static final List<TimeEntryStatus> OPEN_STATUSES =
            List.of(TimeEntryStatus.CLOCKED_IN, TimeEntryStatus.ON_BREAK);

    private final TimeEntryRepository timeEntryRepository;
    private final BreakEntryRepository breakEntryRepository;

    @Scheduled(cron = "${app.autoclose-cron:0 15 7 * * *}",
               zone = "${app.timezone:America/Argentina/Buenos_Aires}")
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void closeForgottenEntries() {

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

        List<TimeEntry> forgotten = timeEntryRepository
                .findByStatusInAndClockInBefore(OPEN_STATUSES, startOfToday);

        if (forgotten.isEmpty()) {
            return;
        }

        for (TimeEntry entry : forgotten) {

            // Cerrar cualquier break que haya quedado abierto (duracion 0).
            if (entry.getBreaks() != null) {
                for (BreakEntry b : entry.getBreaks()) {
                    if (b.getBreakStatus() == BreakStatus.ON_BREAK) {
                        b.setBreakEnd(b.getBreakStart());
                        b.setDurationMinutes(0L);
                        b.setBreakStatus(BreakStatus.FINISHED);
                        breakEntryRepository.save(b);
                    }
                }
            }

            entry.setClockOut(entry.getClockIn()); // 0 horas hasta que el admin corrija
            entry.setStatus(TimeEntryStatus.FINISHED);
            entry.setAutoClosed(true);
            timeEntryRepository.save(entry);
        }

        log.warn("Auto-cerradas {} jornada(s) olvidada(s) de dias anteriores. " +
                "Corregir los horarios reales desde el dashboard de admin.", forgotten.size());
    }
}
