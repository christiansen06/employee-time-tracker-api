-- Pagos realizados: cierran el periodo liquidado de un empleado.
CREATE TABLE payments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    from_date DATE NOT NULL,
    to_date DATE NOT NULL,
    worked_minutes BIGINT NOT NULL,
    double_minutes BIGINT NOT NULL,
    payable_minutes BIGINT NOT NULL,
    hourly_rate DECIMAL(10,2),
    amount DECIMAL(12,2),
    created_by VARCHAR(255),
    created_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_payments_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
) ENGINE=InnoDB;

CREATE INDEX idx_payments_employee ON payments (employee_id, from_date, to_date);
