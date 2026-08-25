# HR Payroll System

A full-stack HR payroll management system for handling **employees, salaries, leave, payslips, payroll processing, and automatic email delivery**.

Built with:

* **Backend:** Java 17, Spring Boot 3, Spring Security, JWT, JPA/Hibernate, Flyway
* **Frontend:** React, Vite
* **Database:** PostgreSQL
* **Authentication:** JWT + BCrypt
* **PDF Generation:** iText
* **Email:** JavaMailSender / SMTP
* **Deployment:** Docker Compose

---

## 📌 Features

* Employee management
* Role-based access control
* Employee vs HR/Admin dashboards
* JWT-based authentication
* Salary structure management
* Employee salary history
* Leave application and approval workflow
* Automatic leave balance allocation
* Monthly payroll processing
* PF, ESI, and PT deductions
* Automatic PDF payslip generation
* Automatic payslip email delivery
* Email delivery success/failure logging
* Payroll run history
* Leave information included in payslips
* Retry failed payslip emails
* HR dashboard with payroll and leave statistics

---

## 📁 Project Structure

```text
hr-payroll-system/
├── backend/                 # Spring Boot API
│   ├── Java 17
│   ├── JPA / Hibernate
│   ├── Spring Security + JWT
│   └── Flyway migrations
│
├── frontend/                # React + Vite SPA
│   └── Role-based UI for Employee / HR / Admin
│
└── docker-compose.yml       # PostgreSQL + Backend configuration
```

---

# 🚀 Getting Started

## Option 1: Run with Docker

From the project root:

```bash
docker compose up --build
```

Once the containers are running:

| Service     | URL / Details         |
| ----------- | --------------------- |
| Backend     | http://localhost:8080 |
| PostgreSQL  | localhost:5432        |
| Database    | `hrms_db`             |
| DB User     | `hrms_user`           |
| DB Password | `hrms_pass`           |

### SMTP Configuration

Set your SMTP credentials using:

```text
MAIL_USERNAME
MAIL_PASSWORD
```

in `docker-compose.yml`.

> **Gmail users:** Use a Gmail **App Password** instead of your normal Gmail password.

---

# 💻 Running the Frontend

The frontend can be run separately:

```bash
cd frontend
npm install
npm run dev
```

Frontend:

```text
http://localhost:3000
```

The Vite development server proxies `/api` requests to the backend running on port `8080`.

---

# 🔧 Running the Backend Manually

Navigate to the backend:

```bash
cd backend
```

Set the required environment variables:

```bash
export DB_USERNAME=hrms_user
export DB_PASSWORD=hrms_pass
export JWT_SECRET=$(openssl rand -base64 32)
export MAIL_USERNAME=you@gmail.com
export MAIL_PASSWORD=app-password
```

Start Spring Boot:

```bash
mvn spring-boot:run
```

### Database Initialization

Flyway automatically creates the database schema and seeds the initial data during the first application startup.

The migration is located at:

```text
backend/src/main/resources/db/migration/V1__init_schema.sql
```

The initial migration creates:

* Database tables
* Leave types
* Departments
* Required relationships and constraints

The default leave types are:

| Leave Type        | Annual Quota |
| ----------------- | -----------: |
| CL — Casual Leave |           12 |
| SL — Sick Leave   |           12 |
| EL — Earned Leave |           15 |

---

# 👤 Creating the First HR/Admin User

There is **no public signup endpoint**.

This is intentional because HR and employee information should not be created through unrestricted self-registration.

Create the first administrator directly in the database.

```sql
INSERT INTO employees (
    employee_code,
    first_name,
    last_name,
    email,
    password_hash,
    role,
    date_of_joining,
    active
)
VALUES (
    'EMP1000',
    'System',
    'Admin',
    'admin@company.com',
    '$2a$10$<bcrypt-hash-of-your-password>',
    'ADMIN',
    CURRENT_DATE,
    true
);
```

### Generate a BCrypt Password

Generate a BCrypt hash using a BCrypt-compatible tool.

Alternatively, temporarily expose:

```java
passwordEncoder.encode(...)
```

through a temporary `CommandLineRunner`.

After the first administrator is created, HR/Admin users can create other employees using:

```http
POST /api/employees
```

Creating an employee automatically allocates that year's leave balances:

```text
CL = 12
SL = 12
EL = 15
```

---

# 🏖️ 1. Leave Management

The system provides fixed annual leave quotas:

| Leave Type | Annual Quota |
| ---------- | -----------: |
| CL         |           12 |
| SL         |           12 |
| EL         |           15 |

When a new employee is created, the system automatically creates a `leave_balances` record for each leave type for the current year.

Leave balances are available through:

```http
GET /api/employees/me
```

and:

```http
GET /api/dashboard
```

---

## Leave Approval & Balance Logic

Leave balances are deducted **only when a leave request is approved**.

They are **not deducted when the request is submitted**.

This prevents multiple pending requests from incorrectly reducing the employee's available balance.

The approval process performs a final balance check before deducting leave.

### Example

```text
Employee balance: 10 CL

Request 1 → Pending → Balance remains 10
Request 2 → Pending → Balance remains 10

Request 1 → Approved → Balance becomes 8
Request 2 → Approved → Checked against remaining balance
```

If an already-approved leave request is cancelled, the deducted balance is restored.

---

# 💰 2. Salary Structures

Salary structures support full CRUD operations:

```http
POST   /api/salary-structures
PUT    /api/salary-structures/{id}
DELETE /api/salary-structures/{id}
```

Deleting a salary structure performs a **soft deactivation** rather than permanently removing it.

This preserves historical salary information.

### Gross Salary Calculation

Gross salary is calculated as:

```text
Gross Salary =
    Basic
  + HRA
  + Special Allowance
```

Gross salary is calculated on the entity and is not redundantly stored, except when included in an immutable historical snapshot.

---

## Assigning a Salary Structure

Assign a salary structure to an employee using:

```http
POST /api/salary-structures/assign
```

When a salary structure is assigned:

1. The employee is linked to the new salary structure.
2. A new record is created in `employee_salary_history`.
3. The previous salary history record is closed using `effective_to`.
4. Historical salary information remains immutable.

View an employee's complete salary history:

```http
GET /api/salary-structures/employee/{id}/history
```

---

# 🧾 3. Leave & Payroll Integration

Payroll integrates directly with the leave management system.

When generating a payslip, the system calculates:

1. Leave used during the current payroll period.
2. Remaining leave balance for the current year.

This information appears in the payslip's **Leave Summary** section.

For example:

```text
Leave Summary

CL Used This Period:        2
CL Remaining This Year:    10

SL Used This Period:        1
SL Remaining This Year:    11

EL Used This Period:        3
EL Remaining This Year:    12
```

This ensures that the payslip reflects both:

* **Period leave usage**
* **Year-to-date remaining balance**

---

# 🧮 4. Monthly Payroll Processing

The main payroll process is:

```java
PayslipService.runMonthlyPayroll(month, year)
```

The process performs the following steps:

### Step 1 — Find Active Employees

The system loops through every active employee.

### Step 2 — Load Salary Information

The employee's current salary structure is retrieved.

### Step 3 — Calculate Salary

The system calculates:

* Basic salary
* HRA
* Special allowance
* Gross salary
* PF
* ESI
* Professional Tax
* Net salary

### Step 4 — Calculate Leave Summary

Leave usage for the payroll period and remaining annual balances are calculated.

### Step 5 — Generate Payslip

A `Payslip` database record is created.

### Step 6 — Generate PDF

A PDF payslip is generated using `PdfGeneratorService` and iText.

The PDF contains:

* Employee ID
* Employee Name
* Department
* Designation
* Pay Period
* Earnings
* Deductions
* Leave Summary
* Net Pay

### Step 7 — Send Email

The generated PDF is emailed to the employee as an attachment using:

```text
EmailService
JavaMailSender
```

### Step 8 — Log Email Result

Every email delivery is logged in:

```text
email_logs
```

Each employee receives a `SUCCESS` or `FAILED` email status.

A failed email **does not stop payroll processing for other employees**.

### Step 9 — Save Payroll Run

The complete payroll operation is recorded in:

```text
PayrollRun
```

This information is used for dashboard reporting.

---

# ⏰ Automatic Payroll Scheduling

Payroll automatically runs on the **1st day of every month at 02:00 AM**.

It processes the **previous month's payroll**.

The scheduler is implemented through:

```text
PayrollScheduler
```

The cron expression is configurable in:

```text
application.yml
```

---

## ▶️ Manual Payroll Run

HR can also trigger payroll manually through:

```http
POST /api/payroll/run
```

This is available from the HR **Run Payroll** screen.

---

## 🔄 Retry Failed Emails

If a payslip email fails, HR can retry it individually:

```http
POST /api/payroll/payslips/{id}/retry-email
```

This allows email delivery failures to be corrected without rerunning the entire payroll.

---

# 📊 5. HR Dashboard

The HR dashboard is powered by:

```http
GET /api/dashboard
```

It provides:

* Total active employees
* Payslips processed for the current period
* Pending payroll
* Successfully sent payslips
* Failed email deliveries
* Pending leave approvals
* Organization-wide leave statistics

### Leave Summary

The dashboard provides allocated, used, and remaining balances for:

```text
CL
SL
EL
```

---

# 🔐 Security

The application uses **Spring Security + stateless JWT authentication**.

Requests use:

```http
Authorization: Bearer <token>
```

### Password Security

Passwords are securely hashed using:

```text
BCrypt
```

### Role-Based Access Control

The system supports:

```text
EMPLOYEE
HR
ADMIN
```

Authorization is enforced on the backend through `SecurityConfig`.

The frontend also hides HR/Admin-only navigation and routes, but this is only a UI convenience.

> **The backend API is the actual security boundary.**

---

## Employee Access

Employees can:

* View their own profile
* View their leave balance
* Apply for leave
* View their leave history
* View their own payslips
* Download their own payslips

Employees cannot access other employees' payslips.

---

## HR/Admin Access

HR/Admin users can:

* Manage employees
* Manage salary structures
* Assign salaries
* View salary history
* Approve/reject leave
* Run payroll
* View payslips
* Retry failed emails
* View dashboard statistics

---

# 🌐 API Summary

| Area                  | Endpoints                                          |
| --------------------- | -------------------------------------------------- |
| **Authentication**    | `POST /api/auth/login`                             |
| **Employees**         | `GET/POST/PUT/DELETE /api/employees`               |
| **My Profile**        | `GET /api/employees/me`                            |
| **Leave**             | `POST /api/leave/apply`                            |
| **Leave History**     | `GET /api/leave/my-history`                        |
| **Leave Balance**     | `GET /api/leave/my-balance`                        |
| **Cancel Leave**      | `POST /api/leave/{id}/cancel`                      |
| **Pending Leave**     | `GET /api/leave/pending`                           |
| **Approve Leave**     | `POST /api/leave/approve/{id}`                     |
| **Reject Leave**      | `POST /api/leave/reject/{id}`                      |
| **Salary Structures** | `GET/POST/PUT/DELETE /api/salary-structures`       |
| **Assign Salary**     | `POST /api/salary-structures/assign`               |
| **Current Salary**    | `GET /api/salary-structures/employee/{id}/current` |
| **Salary History**    | `GET /api/salary-structures/employee/{id}/history` |
| **Run Payroll**       | `POST /api/payroll/run`                            |
| **My Payslips**       | `GET /api/payroll/payslips/my`                     |
| **Employee Payslips** | `GET /api/payroll/payslips/employee/{id}`          |
| **Payslip Details**   | `GET /api/payroll/payslips/{id}`                   |
| **Download Payslip**  | `GET /api/payroll/payslips/{id}/download`          |
| **Retry Email**       | `POST /api/payroll/payslips/{id}/retry-email`      |
| **Dashboard**         | `GET /api/dashboard`                               |

---

# 🛠️ Production Hardening

The current system is suitable for development/demo use. Before deploying to a larger production environment, consider the following improvements:

### Authentication

* Add refresh tokens.
* Use short-lived access tokens.
* Rotate refresh tokens where appropriate.

### Payslip Storage

Currently generated payslips are stored locally.

For production, consider moving:

```text
generated-payslips/
```

to cloud object storage such as:

* Amazon S3
* Azure Blob Storage
* Google Cloud Storage

### API Scalability

Add pagination to:

* Employee lists
* Payslip lists
* Leave history
* Salary history

This becomes important for organizations with a large number of employees.

### Testing

Add integration tests covering:

* Leave approval balance calculations
* Leave cancellation and balance restoration
* Salary history
* Payroll calculations
* Payroll idempotency
* Failed email handling

### Email Scalability

Currently emails are sent synchronously during payroll processing.

For larger organizations, consider using a message queue such as:

```text
RabbitMQ
SQS
Kafka
```

This allows email processing to happen asynchronously without slowing down the payroll process.

---

# 🧱 Technology Stack

| Layer              | Technology              |
| ------------------ | ----------------------- |
| Backend            | Java 17                 |
| Framework          | Spring Boot 3           |
| Security           | Spring Security + JWT   |
| ORM                | JPA / Hibernate         |
| Database           | PostgreSQL              |
| Database Migration | Flyway                  |
| Frontend           | React                   |
| Build Tool         | Vite                    |
| PDF Generation     | iText                   |
| Email              | JavaMailSender          |
| Password Hashing   | BCrypt                  |
| Containerization   | Docker / Docker Compose |

---

# 📌 Important Notes

* There is intentionally **no public employee signup**.
* The first `ADMIN` user must be created directly in the database.
* Leave balances are deducted only after approval.
* Approved leave cancellations restore the deducted balance.
* Salary history is immutable.
* Salary structures are soft-deactivated rather than permanently deleted.
* Failed emails do not stop the payroll process.
* Failed payslip emails can be retried individually.
* Backend authorization is the primary security boundary.
* Payroll can run automatically or manually.
* Payslips contain both payroll-period leave usage and yearly remaining leave balance.

---

# 🚀 Quick Start Summary

```bash
# 1. Start backend + PostgreSQL
docker compose up --build

# 2. Start frontend
cd frontend
npm install
npm run dev
```

Then open:

```text
Frontend → http://localhost:3000
Backend  → http://localhost:8080
```

Create the initial `ADMIN` account, log in, and use the HR dashboard to create employees, configure salaries, manage leave, and run payroll.
