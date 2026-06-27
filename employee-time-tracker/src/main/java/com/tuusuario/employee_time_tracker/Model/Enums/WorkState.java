package com.tuusuario.employee_time_tracker.Model.Enums;

/**
 * Estado de fichaje actual de un empleado, calculado para el frontend
 * para saber que botones habilitar.
 */
public enum WorkState {
    /** Sin jornada abierta: solo puede hacer Clock In. */
    NO_SHIFT,
    /** Fichado y trabajando: puede iniciar break o hacer Clock Out. */
    CLOCKED_IN,
    /** En break: solo puede terminar el break. */
    ON_BREAK
}
