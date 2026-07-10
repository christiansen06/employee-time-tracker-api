-- Integridad: un email no puede repetirse entre empleados.
-- Si esta migracion falla en una base existente, hay emails duplicados
-- que deben corregirse a mano antes de reintentar.
ALTER TABLE employees ADD CONSTRAINT uk_employees_email UNIQUE (email);

-- Indices para las consultas mas frecuentes (estado actual y reportes por rango).
CREATE INDEX idx_time_entries_employee_status ON time_entries (employee_id, status);
CREATE INDEX idx_time_entries_clock_in ON time_entries (clock_in);
CREATE INDEX idx_break_entries_status ON break_entries (break_status);
