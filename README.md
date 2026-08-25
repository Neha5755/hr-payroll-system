# HR Payroll System — Salary, Payslips, Leave & Auto-Email

Full-stack module: Java 17 / Spring Boot 3 backend + React frontend, PostgreSQL, JWT auth,
automatic PDF payslip generation and email delivery.

## Structure
```
hr-payroll-system/
├── backend/     Spring Boot API (Java 17, JPA/Hibernate, Spring Security + JWT, Flyway)
├── frontend/    React (Vite) SPA, role-based UI for Employee vs HR/Admin
└── docker-compose.yml
```

## Quick start (Docker)
```bash
docker compose up --build
```
- Backend: http://localhost:8080
- Postgres: localhost:5432 (hrms_db / hrms_user / hrms_pass)
- Set real SMTP creds via env vars `MAIL_USERNAME` / `MAIL_PASSWORD` in docker-compose.yml
  (for Gmail, use an App Password, not your normal password).

Frontend (run separately):
```bash
cd frontend
npm install
npm run dev      # http://localhost:3000, proxies /api to :8080
```

## Manual backend run
```bash
cd backend
export DB_USERNAME=hrms_user DB_PASSWORD=hrms_pass
export JWT_SECRET=$(openssl rand -base64 32)
export MAIL_USERNAME=you@gmail.com MAIL_PASSWORD=app-password
mvn spring-boot:run
```
Flyway auto-creates the schema and seeds the three leave types (CL/SL/EL) and departments
on first boot (see `src/main/resources/db/migration/V1__init_schema.sql`).

## Creating the first HR/Admin user
There's no public signup endpoint (by design — HR data shouldn't be self-service).
Insert the first admin directly, e.g.:
```sql
INSERT INTO employees (employee_code, first_name, last_name, email, password_hash, role, date_of_joining, active)
VALUES ('EMP1000', 'System', 'Admin', 'admin@company.com',
        '$2a$10$<bcrypt-hash-of-your-password>', 'ADMIN', CURRENT_DATE, true);
```
Generate a bcrypt hash with any bcrypt tool, or temporarily expose `passwordEncoder.encode(...)`
in a throwaway `CommandLineRunner`. After that, HR/Admin can create every other employee via
`POST /api/employees`, which also auto-allocates that year's CL(12)/SL(12)/EL(15) balances.

## 1. Leave policy
- Fixed annual quotas seeded in `leave_types`: CL 12, SL 12, EL 15.
- Every new employee gets a `leave_balances` row per type per year automatically.
- Balances surface on `GET /api/employees/me` (profile) and `GET /api/dashboard` (org summary).

## 2. Salary structures
- Full CRUD: `POST/PUT/DELETE /api/salary-structures` (delete = soft-deactivate, so history
  stays intact for anyone previously on that structure).
- Gross = Basic + HRA + Special Allowance, computed on the entity, never stored redundantly
  except inside the immutable history snapshot.
- `POST /api/salary-structures/assign` points an employee at a structure and writes an
  **immutable** row to `employee_salary_history` (closes the previous row's `effective_to`
  date). Full history: `GET /api/salary-structures/employee/{id}/history`.

## 3. Leave–payroll integration
- Balances are only decremented at approval time (`LeaveService.approveLeave`), never on
  submission — so two pending overlapping requests can't double-block each other, but
  approval is still hard-checked against remaining balance.
- Payslip generation pulls: (a) days of *that leave type* used within the pay period, and
  (b) the running remaining balance for the year, so the payslip's "Leave Summary" section
  always reflects both period usage and year-to-date balance.
- Cancelling an already-approved request restores the deducted balance.

## 4 & 5. Payslip generation + auto-email
`PayslipService.runMonthlyPayroll(month, year)`:
1. Loops every active employee
2. Pulls their **current** salary structure snapshot, computes PF/ESI/PT deductions and net
3. Builds the leave summary for that period
4. Saves the `Payslip` row, renders a PDF (`PdfGeneratorService`, iText) with Employee ID,
   Name, Department, Designation, Pay Period, Earnings, Deductions, Leave Summary, Net Pay
5. Emails the PDF as an attachment (`EmailService`, JavaMailSender)
6. Logs SUCCESS/FAILED per employee to `email_logs` (never throws — a failed email doesn't
   block the run for other employees)
7. Rolls everything into a `PayrollRun` row for dashboard reporting

Runs automatically on the 1st of every month at 02:00 for the *previous* month
(`PayrollScheduler`, cron configurable in `application.yml`), or on-demand via
`POST /api/payroll/run` from the HR "Run Payroll" screen. Failed deliveries can be retried
individually via `POST /api/payroll/payslips/{id}/retry-email`.

## 6. HR dashboard
`GET /api/dashboard` returns: total active employees, payslips processed this period,
pending payroll, payslips sent successfully, failed deliveries, pending leave approvals,
and an org-wide CL/SL/EL allocated/used/remaining summary.

## Security
- Spring Security + stateless JWT (`Authorization: Bearer <token>`), BCrypt password hashing.
- Role-based access enforced server-side in `SecurityConfig` (EMPLOYEE vs HR vs ADMIN) —
  the frontend also hides HR-only nav/routes, but the API is the actual boundary.
- Employees can only view/download their own payslips; HR/Admin can view anyone's.

## API summary
| Area | Endpoints |
|---|---|
| Auth | `POST /api/auth/login` |
| Employees | `GET/POST/PUT/DELETE /api/employees`, `GET /api/employees/me` |
| Leave | `POST /api/leave/apply`, `/my-history`, `/my-balance`, `/{id}/cancel`, `/pending`, `/approve/{id}`, `/reject/{id}` |
| Salary | `GET/POST/PUT/DELETE /api/salary-structures`, `/assign`, `/employee/{id}/current`, `/employee/{id}/history` |
| Payroll | `POST /api/payroll/run`, `GET /api/payroll/payslips/my`, `/employee/{id}`, `/{id}`, `/{id}/download`, `POST /{id}/retry-email` |
| Dashboard | `GET /api/dashboard` |

## Notes / production hardening still worth doing
- Add refresh tokens (access-token-only JWT is fine for a demo, short-lived in prod).
- Move `generated-payslips/` to S3/blob storage instead of local disk.
- Add pagination to employee/payslip list endpoints for large orgs.
- Add integration tests around leave-approval balance math and payroll idempotency.
- Consider a message queue (SQS/RabbitMQ) for email sending at scale instead of inline sync send.
