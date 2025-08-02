# Database Design Documentation for Multi-Platform Banking System (Updated)

## Overview

This document defines the entities, their attributes, and relationships to be used in the banking system database. It
covers core banking, user management, security, AI models, customer support, and system configuration.

---

## Entities and Attributes

### 1. Customer

Represents a bank customer.

| Attribute       | Type       | Description                |
|-----------------|------------|----------------------------|
| CustomerID (PK) | UUID / INT | Unique customer identifier |
| FirstName       | VARCHAR    | Customer's first name      |
| LastName        | VARCHAR    | Customer's last name       |
| Gender          | ENUM       | Male, Female, Other        |
| Email           | VARCHAR    | Unique email address       |
| Phone           | VARCHAR    | Contact phone number       |
| Address         | TEXT       | Residential address        |
| DateOfBirth     | DATE       | Birth date                 |
| Status          | ENUM       | Active, Inactive           |
| CreatedAt       | DATETIME   | Record creation timestamp  |
| UpdatedAt       | DATETIME   | Last update timestamp      |

---

### 2. Account

Bank accounts owned by customers.

| Attribute       | Type       | Description                    |
|-----------------|------------|--------------------------------|
| AccountID (PK)  | UUID / INT | Unique account identifier      |
| AccountNumber   | VARCHAR    | Unique account number          |
| AccountType     | ENUM       | Savings, Checking, etc.        |
| AccountStatus   | ENUM       | Active, Closed, Frozen         |
| Balance         | DECIMAL    | Current balance                |
| CustomerID (FK) | UUID / INT | Owner customer reference       |
| BranchID (FK)   | INT        | Reference to branch (nullable) |
| CreatedAt       | DATETIME   | Record creation timestamp      |
| UpdatedAt       | DATETIME   | Last update timestamp          |

---

### 3. Transaction

Monetary transactions (deposit, withdrawal, transfer).

| Attribute          | Type       | Description                          |
|--------------------|------------|--------------------------------------|
| TransactionID (PK) | UUID / INT | Unique transaction identifier        |
| AccountID (FK)     | UUID / INT | Associated account                   |
| TransactionType    | ENUM       | Deposit, Withdrawal, Transfer        |
| Amount             | DECIMAL    | Transaction amount                   |
| Timestamp          | DATETIME   | When transaction occurred            |
| ReferenceNumber    | VARCHAR    | External reference number (optional) |
| Description        | TEXT       | Optional description                 |

---

### 4. Loan

Customer loan applications and statuses.

| Attribute        | Type       | Description                       |
|------------------|------------|-----------------------------------|
| LoanID (PK)      | UUID / INT | Unique loan identifier            |
| CustomerID (FK)  | UUID / INT | Applicant customer                |
| LoanType         | ENUM       | Personal, Mortgage, etc.          |
| Amount           | DECIMAL    | Loan amount                       |
| InterestRate     | DECIMAL    | Interest percentage               |
| LoanTerm         | INT        | Loan duration in months or years  |
| PaymentFrequency | ENUM       | Monthly, Quarterly, Yearly        |
| RemainingBalance | DECIMAL    | Current outstanding loan balance  |
| Status           | ENUM       | Pending, Approved, Rejected, Paid |
| ApplicationDate  | DATETIME   | Date of application               |
| ApprovalDate     | DATETIME   | Date of approval                  |
| DueDate          | DATETIME   | Loan repayment due date           |

---

### 5. Employee

Bank employees (Teller, Manager, Admin).

| Attribute       | Type       | Description                   |
|-----------------|------------|-------------------------------|
| EmployeeID (PK) | UUID / INT | Unique employee identifier    |
| FirstName       | VARCHAR    | Employee first name           |
| LastName        | VARCHAR    | Employee last name            |
| Email           | VARCHAR    | Unique email address          |
| Phone           | VARCHAR    | Contact phone number          |
| RoleID (FK)     | INT        | Role reference                |
| Department      | VARCHAR    | Department name (optional)    |
| ManagerID (FK)  | UUID / INT | Employee's manager (nullable) |
| HireDate        | DATETIME   | Date hired                    |
| Status          | ENUM       | Active, Inactive              |

---

### 6. Role

Defines user roles and permissions.

| Attribute   | Type    | Description                                     |
|-------------|---------|-------------------------------------------------|
| RoleID (PK) | INT     | Unique role identifier                          |
| RoleName    | VARCHAR | Name of role (Customer, Teller, Manager, Admin) |
| Description | TEXT    | Description of the role                         |
| Permissions | JSON    | (Optional) List or JSON of permissions for RBAC |

---

### 7. User

Authentication accounts for Customers and Employees.

| Attribute                 | Type       | Description                   |
|---------------------------|------------|-------------------------------|
| UserID (PK)               | UUID / INT | Unique user identifier        |
| Username                  | VARCHAR    | Unique login username         |
| PasswordHash              | VARCHAR    | Hashed password (secure)      |
| Email                     | VARCHAR    | Unique email address          |
| RoleID (FK)               | INT        | Role reference                |
| CustomerID (FK, nullable) | UUID / INT | Customer reference (nullable) |
| EmployeeID (FK, nullable) | UUID / INT | Employee reference (nullable) |
| IsActive                  | BOOLEAN    | Account active status         |
| LastLoginAt               | DATETIME   | Timestamp of last login       |
| CreatedAt                 | DATETIME   | Account creation timestamp    |

---

### 8. Notification

Alerts sent to customers.

| Attribute           | Type       | Description                    |
|---------------------|------------|--------------------------------|
| NotificationID (PK) | UUID / INT | Unique notification identifier |
| CustomerID (FK)     | UUID / INT | Recipient customer             |
| Message             | TEXT       | Notification message content   |
| NotificationType    | ENUM       | Email, SMS, In-App             |
| CreatedAt           | DATETIME   | Timestamp                      |
| SentAt              | DATETIME   | When notification was sent     |
| DeliveryStatus      | ENUM       | Pending, Sent, Failed          |
| ReadStatus          | BOOLEAN    | Whether notification was read  |

---

### 9. AuditLog

Records actions for security and tracking.

| Attribute       | Type       | Description                         |
|-----------------|------------|-------------------------------------|
| AuditLogID (PK) | UUID / INT | Unique audit log identifier         |
| UserID (FK)     | UUID / INT | User who performed the action       |
| ActionType      | VARCHAR    | Type of action performed            |
| Timestamp       | DATETIME   | When action occurred                |
| IPAddress       | VARCHAR    | IP address related to action        |
| DeviceID (FK)   | UUID / INT | Device related to action (optional) |
| Description     | TEXT       | Details of the action               |

---

### 10. Session

Tracks user login sessions.

| Attribute      | Type       | Description               |
|----------------|------------|---------------------------|
| SessionID (PK) | UUID / INT | Unique session identifier |
| UserID (FK)    | UUID / INT | User reference            |
| Token          | VARCHAR    | Session token             |
| LoginTime      | DATETIME   | Session start time        |
| LogoutTime     | DATETIME   | Session end time          |
| ExpiryTime     | DATETIME   | Session expiry time       |
| IsActive       | BOOLEAN    | Whether session is active |
| IPAddress      | VARCHAR    | IP address of user        |

---

### 11. CreditScore

Stores credit scores for customers (used in loan approval).

| Attribute          | Type       | Description                   |
|--------------------|------------|-------------------------------|
| CreditScoreID (PK) | UUID / INT | Unique credit score record    |
| CustomerID (FK)    | UUID / INT | Customer reference            |
| Score              | INT        | Credit score value            |
| EvaluationDate     | DATETIME   | Date of evaluation            |
| Source             | VARCHAR    | Source of credit score        |
| Notes              | TEXT       | Additional remarks (optional) |

---

### 12. AIModel

Stores AI/ML models info for fraud detection, credit scoring.

| Attribute      | Type       | Description                        |
|----------------|------------|------------------------------------|
| AIModelID (PK) | UUID / INT | Unique AI model identifier         |
| ModelName      | VARCHAR    | Name of the model                  |
| ModelVersion   | VARCHAR    | Version identifier                 |
| ModelType      | VARCHAR    | Type (Fraud, Credit Scoring, etc.) |
| Status         | ENUM       | Active, Deprecated                 |
| Description    | TEXT       | Model description                  |
| CreatedAt      | DATETIME   | Creation timestamp                 |
| LastTrainedAt  | DATETIME   | Last training timestamp            |

---

### 13. LoanRepayment

Records repayments for loans.

| Attribute                    | Type       | Description                       |
|------------------------------|------------|-----------------------------------|
| RepaymentID (PK)             | UUID / INT | Unique repayment identifier       |
| LoanID (FK)                  | UUID / INT | Loan reference                    |
| TransactionID (FK, nullable) | UUID / INT | Related transaction (optional)    |
| Amount                       | DECIMAL    | Repayment amount                  |
| PaymentDate                  | DATETIME   | Date of repayment                 |
| PaymentMethod                | VARCHAR    | Payment method (cash, card, etc.) |

---

### 14. SystemConfig

Stores system-wide configuration settings.

| Attribute     | Type       | Description                         |
|---------------|------------|-------------------------------------|
| ConfigID (PK) | UUID / INT | Unique configuration ID             |
| ConfigKey     | VARCHAR    | Configuration key                   |
| ConfigValue   | TEXT       | Configuration value                 |
| ConfigType    | VARCHAR    | Data type (string, int, bool, etc.) |
| Description   | TEXT       | Description                         |

---

~~### 15. FileStorage

Stores metadata for uploaded files (e.g., KYC documents).

| Attribute       | Type       | Description                            |
|-----------------|------------|----------------------------------------|
| FileID (PK)     | UUID / INT | Unique file record identifier          |
| CustomerID (FK) | UUID / INT | Customer reference                     |
| FileName        | VARCHAR    | Original file name                     |
| FileType        | VARCHAR    | MIME type or file extension            |
| FileSize        | INT        | File size in bytes (optional)          |
| Checksum        | VARCHAR    | File checksum for integrity (optional) |
| FilePath        | VARCHAR    | Path or URL to stored file             |
| UploadedAt      | DATETIME   | Upload timestamp                       |~~

---

### 16. SupportTicket

Customer service tickets.

| Attribute                           | Type       | Description               |
|-------------------------------------|------------|---------------------------|
| TicketID (PK)                       | UUID / INT | Unique ticket identifier  |
| CustomerID (FK)                     | UUID / INT | Customer reference        |
| Subject                             | VARCHAR    | Ticket subject            |
| Description                         | TEXT       | Detailed description      |
| Status                              | ENUM       | Open, In Progress, Closed |
| Priority                            | ENUM       | Low, Medium, High         |
| AssignedToEmployeeID (FK, nullable) | UUID / INT | Assigned employee         |
| CreatedAt                           | DATETIME   | Creation timestamp        |
| UpdatedAt                           | DATETIME   | Last update timestamp     |

---

### 17. Feedback

Customer feedback on services.

| Attribute       | Type       | Description                |
|-----------------|------------|----------------------------|
| FeedbackID (PK) | UUID / INT | Unique feedback identifier |
| CustomerID (FK) | UUID / INT | Customer reference         |
| FeedbackText    | TEXT       | Feedback content           |
| Rating          | INT        | Rating score (e.g., 1-5)   |
| ResponseStatus  | ENUM       | Pending, Reviewed          |
| SubmittedAt     | DATETIME   | Submission date            |

---

### 18. Branch

Bank branch details.

| Attribute                        | Type       | Description              |
|----------------------------------|------------|--------------------------|
| BranchID (PK)                    | INT        | Unique branch identifier |
| BranchName                       | VARCHAR    | Branch name              |
| Address                          | TEXT       | Branch address           |
| ContactNumber                    | VARCHAR    | Phone number             |
| ManagerEmployeeID (FK, nullable) | UUID / INT | Branch manager employee  |

---

### 19. ScheduledTransaction

Future transactions set by customers.

| Attribute                   | Type       | Description                             |
|-----------------------------|------------|-----------------------------------------|
| ScheduledTransactionID (PK) | UUID / INT | Unique scheduled transaction ID         |
| AccountID (FK)              | UUID / INT | Account reference                       |
| TransactionType             | ENUM       | Deposit, Withdrawal, Transfer           |
| Amount                      | DECIMAL    | Transaction amount                      |
| ScheduledDate               | DATETIME   | When transaction should occur           |
| RecurrencePattern           | VARCHAR    | e.g., Daily, Weekly, Monthly (optional) |
| Status                      | ENUM       | Pending, Completed, Cancelled           |

---

### 20. Device

Stores registered devices for customers (for 2FA, security).

| Attribute       | Type       | Description                     |
|-----------------|------------|---------------------------------|
| DeviceID (PK)   | UUID / INT | Unique device identifier        |
| CustomerID (FK) | UUID / INT | Customer reference              |
| DeviceType      | VARCHAR    | Device type (mobile, desktop)   |
| DeviceToken     | VARCHAR    | Unique device token or ID       |
| RegisteredAt    | DATETIME   | Registration timestamp          |
| LastUsedAt      | DATETIME   | Last usage timestamp (optional) |
| IsActive        | BOOLEAN    | Whether device is active        |

---

### 21. Card

Bank cards linked to accounts (debit/credit).

| Attribute                       | Type       | Description                               |
|---------------------------------|------------|-------------------------------------------|
| CardID (PK)                     | UUID / INT | Unique card identifier                    |
| AccountID (FK)                  | UUID / INT | Linked account                            |
| LinkedCustomerID (FK, nullable) | UUID / INT | Linked customer for multi-account holders |
| CardNumber                      | VARCHAR    | Unique card number                        |
| CardType                        | ENUM       | Debit, Credit, Prepaid                    |
| ExpiryDate                      | DATE       | Card expiry date                          |
| CVV (encrypted)                 | VARCHAR    | Encrypted card security code              |
| Status                          | ENUM       | Active, Blocked, Expired                  |
| IssuedAt                        | DATETIME   | Card issuance date                        |
| CreatedAt                       | DATETIME   | Record creation timestamp                 |
| UpdatedAt                       | DATETIME   | Last update timestamp                     |

---

## Relationships Summary

| Entity 1 | Relationship Type      | Entity 2             | Notes                                     |
|----------|------------------------|----------------------|-------------------------------------------|
| Customer | one to many            | Account              | One customer can have many accounts       |
| Account  | one to many            | Transaction          | One account has many transactions         |
| Customer | one to many            | Loan                 | One customer can have multiple loans      |
| Loan     | one to many            | LoanRepayment        | Loans have multiple repayments            |
| Employee | many to one            | Role                 | Each employee has one role                |
| User     | many to one            | Role                 | Each user assigned one role               |
| User     | one to one (nullable)  | Customer             | User linked optionally to customer        |
| User     | one to one (nullable)  | Employee             | User linked optionally to employee        |
| Customer | one to many            | Notification         | Many notifications per customer           |
| User     | one to many            | AuditLog             | Audit logs related to users               |
| User     | one to many            | Session              | Multiple sessions per user                |
| Customer | one to many            | CreditScore          | Customers may have multiple credit scores |
| Customer | one to many            | FileStorage          | Multiple files (e.g., KYC docs)           |
| Customer | one to many            | SupportTicket        | Multiple support tickets per customer     |
| Customer | one to many            | Feedback             | Multiple feedback entries                 |
| Branch   | one to many (optional) | Account              | Accounts linked to branches               |
| Branch   | one to one (nullable)  | Employee             | Branch manager (optional)                 |
| Account  | one to many            | ScheduledTransaction | Scheduled future transactions             |
| Customer | one to many            | Device               | Multiple registered devices               |
| Account  | one to many            | Card                 | Multiple cards linked to one account      |

---

## Notes

- Primary keys are mostly UUIDs or auto-increment integers depending on implementation.
- Foreign keys ensure referential integrity.
- Enum fields should be clearly defined in implementation.
- Sensitive information (passwords, device tokens) must be stored securely.
- Audit logs and sessions help maintain security and tracking.
- AIModel table supports storing metadata for AI models in the system.
- SystemConfig allows configurable parameters without code changes.
- Indexes should be created on frequently queried fields (e.g., Email, Username, AccountNumber).
- Soft deletes can be implemented via `Status` or `DeletedAt` timestamps.
- Encryption key management is critical for sensitive data like CVV and PasswordHash.

---

*End of Updated Document*
