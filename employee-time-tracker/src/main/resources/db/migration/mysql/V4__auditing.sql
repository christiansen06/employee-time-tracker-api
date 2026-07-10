-- Trazabilidad: cuando se creo/modifico cada fila + bitacora de cambios manuales.

ALTER TABLE employees ADD COLUMN created_at DATETIME(6);
ALTER TABLE employees ADD COLUMN updated_at DATETIME(6);
ALTER TABLE users ADD COLUMN created_at DATETIME(6);
ALTER TABLE users ADD COLUMN updated_at DATETIME(6);
ALTER TABLE time_entries ADD COLUMN created_at DATETIME(6);
ALTER TABLE time_entries ADD COLUMN updated_at DATETIME(6);
ALTER TABLE break_entries ADD COLUMN created_at DATETIME(6);
ALTER TABLE break_entries ADD COLUMN updated_at DATETIME(6);

CREATE TABLE audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT,
    action VARCHAR(20) NOT NULL,
    performed_by VARCHAR(255),
    details VARCHAR(2000),
    created_at DATETIME(6),
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE INDEX idx_audit_log_created_at ON audit_log (created_at);
