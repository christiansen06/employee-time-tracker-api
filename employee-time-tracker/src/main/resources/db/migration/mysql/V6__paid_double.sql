-- Jornadas pagadas al doble (feriados). NULL = false.
ALTER TABLE time_entries ADD COLUMN paid_double BIT(1);
