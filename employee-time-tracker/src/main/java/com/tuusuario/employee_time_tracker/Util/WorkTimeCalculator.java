package com.tuusuario.employee_time_tracker.Util;

import com.tuusuario.employee_time_tracker.Model.Entity.BreakEntry;
import com.tuusuario.employee_time_tracker.Model.Entity.TimeEntry;
import com.tuusuario.employee_time_tracker.Model.Enums.TimeEntryStatus;

import java.time.Duration;

/**
 * Calculo de minutos trabajados, unico para toda la app
 * (tarjeta semanal, reportes y analytics usan el mismo criterio).
 */
public final class WorkTimeCalculator {

    private WorkTimeCalculator() {
    }

    /** Jornada computable: finalizada y con ambos extremos cargados. */
    public static boolean isCountable(TimeEntry entry) {
        return entry.getStatus() == TimeEntryStatus.FINISHED
                && entry.getClockIn() != null
                && entry.getClockOut() != null;
    }

    /** Minutos netos de una jornada: duracion menos breaks. */
    public static long netMinutes(TimeEntry entry) {
        long worked = Duration.between(entry.getClockIn(), entry.getClockOut()).toMinutes();
        return worked - breakMinutes(entry);
    }

    /** Total de minutos de break de una jornada. */
    public static long breakMinutes(TimeEntry entry) {
        if (entry.getBreaks() == null) {
            return 0;
        }
        return entry.getBreaks().stream()
                .filter(b -> b.getDurationMinutes() != null)
                .mapToLong(BreakEntry::getDurationMinutes)
                .sum();
    }
}
