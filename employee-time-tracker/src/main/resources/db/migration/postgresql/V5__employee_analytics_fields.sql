-- Datos para analytics: costo laboral y hora esperada de entrada.
ALTER TABLE employees ADD COLUMN hourly_rate DECIMAL(10,2);
ALTER TABLE employees ADD COLUMN expected_clock_in TIME(6);
ALTER TABLE employees ADD COLUMN weekly_hours_target INT;
