-- =========================================================
-- HR Payroll System - Initial Schema
-- =========================================================

CREATE TABLE departments (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE employees (
    id                  BIGSERIAL PRIMARY KEY,
    employee_code       VARCHAR(20)  NOT NULL UNIQUE,   -- e.g. EMP1001
    first_name          VARCHAR(80)  NOT NULL,
    last_name           VARCHAR(80)  NOT NULL,
    email               VARCHAR(150) NOT NULL UNIQUE,
    password_hash       VARCHAR(255) NOT NULL,
    role                VARCHAR(20)  NOT NULL DEFAULT 'EMPLOYEE',   -- EMPLOYEE | HR | ADMIN
    department_id       BIGINT REFERENCES departments(id),
    designation         VARCHAR(100),
    date_of_joining      DATE NOT NULL,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_employees_department ON employees(department_id);
CREATE INDEX idx_employees_email ON employees(email);

-- =========================================================
-- LEAVE MODULE
-- =========================================================

-- Fixed leave types & their annual quota. Seeded below; editable by HR/Admin if quotas change.
CREATE TABLE leave_types (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(10)  NOT NULL UNIQUE,   -- CL | SL | EL
    name        VARCHAR(50)  NOT NULL,
    annual_quota NUMERIC(5,2) NOT NULL
);

-- One row per employee, per leave type, per calendar year
CREATE TABLE leave_balances (
    id              BIGSERIAL PRIMARY KEY,
    employee_id     BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    leave_type_id   BIGINT NOT NULL REFERENCES leave_types(id),
    leave_year      INT NOT NULL,
    allocated       NUMERIC(5,2) NOT NULL,
    used            NUMERIC(5,2) NOT NULL DEFAULT 0,
    remaining       NUMERIC(5,2) NOT NULL,
    UNIQUE (employee_id, leave_type_id, leave_year)
);

CREATE TABLE leave_requests (
    id              BIGSERIAL PRIMARY KEY,
    employee_id     BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    leave_type_id   BIGINT NOT NULL REFERENCES leave_types(id),
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    days_requested  NUMERIC(5,2) NOT NULL,
    reason          VARCHAR(500),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING | APPROVED | REJECTED | CANCELLED
    approved_by     BIGINT REFERENCES employees(id),
    approved_at     TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_leave_requests_employee ON leave_requests(employee_id);
CREATE INDEX idx_leave_requests_status ON leave_requests(status);

-- =========================================================
-- SALARY MODULE
-- =========================================================

-- Master salary structure definitions (templates), CRUD-able by HR
CREATE TABLE salary_structures (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(100) NOT NULL,      -- e.g. "SDE-1 Band", "Manager Band"
    basic_salary        NUMERIC(12,2) NOT NULL,
    hra                 NUMERIC(12,2) NOT NULL,
    special_allowance   NUMERIC(12,2) NOT NULL,
    pf_percent          NUMERIC(5,2)  NOT NULL DEFAULT 12.00,
    esi_percent         NUMERIC(5,2)  NOT NULL DEFAULT 0.75,
    professional_tax    NUMERIC(12,2) NOT NULL DEFAULT 200.00,
    other_deductions    NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now()
);

-- Which structure is CURRENTLY assigned to an employee
CREATE TABLE employee_salary_structure (
    id                  BIGSERIAL PRIMARY KEY,
    employee_id         BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    salary_structure_id BIGINT NOT NULL REFERENCES salary_structures(id),
    effective_from      DATE NOT NULL,
    UNIQUE (employee_id)
);

-- Full history of every structure ever assigned to an employee, closed-ended by effective_to
CREATE TABLE employee_salary_history (
    id                  BIGSERIAL PRIMARY KEY,
    employee_id         BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    salary_structure_id BIGINT NOT NULL REFERENCES salary_structures(id),
    basic_salary        NUMERIC(12,2) NOT NULL,
    hra                 NUMERIC(12,2) NOT NULL,
    special_allowance   NUMERIC(12,2) NOT NULL,
    gross_salary        NUMERIC(12,2) NOT NULL,
    pf_percent          NUMERIC(5,2)  NOT NULL,
    esi_percent         NUMERIC(5,2)  NOT NULL,
    professional_tax    NUMERIC(12,2) NOT NULL,
    other_deductions    NUMERIC(12,2) NOT NULL,
    effective_from      DATE NOT NULL,
    effective_to        DATE,                          -- NULL = currently active
    changed_by          BIGINT REFERENCES employees(id),
    created_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_salary_history_employee ON employee_salary_history(employee_id);

-- =========================================================
-- PAYSLIP MODULE
-- =========================================================

CREATE TABLE payslips (
    id                  BIGSERIAL PRIMARY KEY,
    employee_id         BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    pay_period_month    INT NOT NULL,        -- 1-12
    pay_period_year     INT NOT NULL,
    basic_salary        NUMERIC(12,2) NOT NULL,
    hra                 NUMERIC(12,2) NOT NULL,
    special_allowance   NUMERIC(12,2) NOT NULL,
    gross_salary        NUMERIC(12,2) NOT NULL,
    pf_deduction        NUMERIC(12,2) NOT NULL,
    esi_deduction       NUMERIC(12,2) NOT NULL,
    professional_tax    NUMERIC(12,2) NOT NULL,
    other_deductions    NUMERIC(12,2) NOT NULL,
    total_deductions    NUMERIC(12,2) NOT NULL,
    net_salary          NUMERIC(12,2) NOT NULL,
    cl_used             NUMERIC(5,2) NOT NULL DEFAULT 0,
    cl_remaining        NUMERIC(5,2) NOT NULL DEFAULT 0,
    sl_used             NUMERIC(5,2) NOT NULL DEFAULT 0,
    sl_remaining        NUMERIC(5,2) NOT NULL DEFAULT 0,
    el_used             NUMERIC(5,2) NOT NULL DEFAULT 0,
    el_remaining        NUMERIC(5,2) NOT NULL DEFAULT 0,
    pdf_path            VARCHAR(500),
    status              VARCHAR(20) NOT NULL DEFAULT 'GENERATED',  -- GENERATED | EMAIL_SENT | EMAIL_FAILED
    generated_at        TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (employee_id, pay_period_month, pay_period_year)
);

CREATE INDEX idx_payslips_period ON payslips(pay_period_year, pay_period_month);
CREATE INDEX idx_payslips_employee ON payslips(employee_id);

CREATE TABLE email_logs (
    id              BIGSERIAL PRIMARY KEY,
    payslip_id      BIGINT NOT NULL REFERENCES payslips(id) ON DELETE CASCADE,
    recipient_email VARCHAR(150) NOT NULL,
    status          VARCHAR(20) NOT NULL,       -- SUCCESS | FAILED
    error_message   VARCHAR(1000),
    attempted_at    TIMESTAMP NOT NULL DEFAULT now(),
    retry_count     INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_email_logs_payslip ON email_logs(payslip_id);
CREATE INDEX idx_email_logs_status ON email_logs(status);

-- A payroll "batch run" so the dashboard can show pending / processed / sent / failed per run
CREATE TABLE payroll_runs (
    id                  BIGSERIAL PRIMARY KEY,
    pay_period_month    INT NOT NULL,
    pay_period_year     INT NOT NULL,
    total_employees     INT NOT NULL DEFAULT 0,
    processed_count     INT NOT NULL DEFAULT 0,
    pending_count       INT NOT NULL DEFAULT 0,
    email_success_count INT NOT NULL DEFAULT 0,
    email_failed_count  INT NOT NULL DEFAULT 0,
    status              VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',  -- IN_PROGRESS | COMPLETED | COMPLETED_WITH_ERRORS
    started_at          TIMESTAMP NOT NULL DEFAULT now(),
    completed_at        TIMESTAMP,
    UNIQUE (pay_period_month, pay_period_year)
);

-- =========================================================
-- SEED DATA
-- =========================================================

INSERT INTO leave_types (code, name, annual_quota) VALUES
    ('CL', 'Casual Leave', 12),
    ('SL', 'Sick Leave', 12),
    ('EL', 'Earned Leave', 15);

INSERT INTO departments (name) VALUES
    ('Engineering'), ('Human Resources'), ('Finance'), ('Sales'), ('Operations');
