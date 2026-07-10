-- Esquema base (equivalente al que generaba Hibernate con ddl-auto=update).
-- Las bases existentes se adoptan via baseline-on-migrate y NO ejecutan este script.

CREATE TABLE employees (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    position VARCHAR(255) NOT NULL,
    active BIT(1) NOT NULL,
    pin_hash VARCHAR(255),
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20),
    employee_id BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_employee UNIQUE (employee_id),
    CONSTRAINT fk_users_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
) ENGINE=InnoDB;

CREATE TABLE time_entries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    clock_in DATETIME(6),
    clock_out DATETIME(6),
    status ENUM('CLOCKED_IN','ON_BREAK','FINISHED'),
    auto_closed BIT(1),
    employee_id BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_time_entries_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
) ENGINE=InnoDB;

CREATE TABLE break_entries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    break_start DATETIME(6),
    break_end DATETIME(6),
    break_status ENUM('ON_BREAK','FINISHED'),
    duration BIGINT,
    time_entry_id BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_break_entries_time_entry FOREIGN KEY (time_entry_id) REFERENCES time_entries (id)
) ENGINE=InnoDB;
