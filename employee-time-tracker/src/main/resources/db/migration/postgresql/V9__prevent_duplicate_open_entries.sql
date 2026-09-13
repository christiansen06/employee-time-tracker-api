-- Cierra la condicion de carrera de doble clock-in / doble break: si dos
-- toques casi simultaneos pasan el chequeo de la app antes de que
-- cualquiera guarde, la base es la ultima linea de defensa.
--
-- Nunca dos jornadas abiertas (CLOCKED_IN/ON_BREAK) para el mismo empleado.
CREATE UNIQUE INDEX uq_time_entries_open_per_employee
    ON time_entries (employee_id)
    WHERE status IN ('CLOCKED_IN', 'ON_BREAK');

-- Nunca dos breaks activos (ON_BREAK) en la misma jornada.
CREATE UNIQUE INDEX uq_break_entries_open_per_shift
    ON break_entries (time_entry_id)
    WHERE break_status = 'ON_BREAK';
