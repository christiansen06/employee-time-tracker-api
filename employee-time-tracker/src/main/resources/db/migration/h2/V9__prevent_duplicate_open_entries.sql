-- Cierra la condicion de carrera de doble clock-in / doble break: si dos
-- toques casi simultaneos pasan el chequeo de la app antes de que
-- cualquiera guarde, la base es la ultima linea de defensa.
--
-- H2 no soporta indice unico parcial (WHERE) como Postgres: se usa una
-- columna generada que vale TRUE solo en el caso "abierto" y NULL en el
-- resto (los NULL no cuentan como duplicados en un indice unico), con el
-- indice unico sobre esa columna. Mismo truco que en MySQL.
ALTER TABLE time_entries
    ADD COLUMN open_marker BOOLEAN
        GENERATED ALWAYS AS (CASE WHEN status IN ('CLOCKED_IN', 'ON_BREAK') THEN TRUE END);
CREATE UNIQUE INDEX uq_time_entries_open_per_employee
    ON time_entries (employee_id, open_marker);

ALTER TABLE break_entries
    ADD COLUMN open_marker BOOLEAN
        GENERATED ALWAYS AS (CASE WHEN break_status = 'ON_BREAK' THEN TRUE END);
CREATE UNIQUE INDEX uq_break_entries_open_per_shift
    ON break_entries (time_entry_id, open_marker);
