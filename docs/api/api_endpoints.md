
# REST API Endpoints — Multi-Platform Banking System

**API Version:** `v1`  
**Base URL Format:** `/api/v1/...`  
**Format:** JSON request/response  
**Authentication:** JWT Token (Bearer)  
**Content-Type:** `application/json`  
**Rate Limits:** Example - 100 requests/minute per user (customizable per endpoint)

---

## Authentication & User Management

### POST `/auth/register`
Register a new customer.

**Request Body:**

```json
{
  "username": "string",
  "email": "string",
  "password": "string",
  "fullName": "string"
}
```

**Responses:**
- `201 Created` - User registered successfully.
- `400 Bad Request` - Validation errors (e.g., email already exists).

**Roles Allowed:** Public  
**Rate Limit:** 5 requests/minute per IP

---

### POST `/auth/login`
Authenticate user and return JWT token.

**Request Body:**

```json
{
  "email": "string",
  "password": "string"
}
```

**Responses:**
- `200 OK`

```json
{
  "token": "jwt-token-string",
  "expiresIn": 3600
}
```

- `401 Unauthorized` - Invalid credentials.

**Roles Allowed:** Public  
**Rate Limit:** 10 requests/minute per IP

---

### POST `/auth/logout`
Logout user and invalidate current JWT.

**Request Body:** None

**Responses:**
- `200 OK` - Successfully logged out.

**Roles Allowed:** Authenticated users

---

### POST `/auth/change-password`
Change password for logged-in user.

**Request Body:**

```json
{
  "oldPassword": "string",
  "newPassword": "string"
}
```

**Responses:**
- `200 OK` - Password changed successfully.
- `400 Bad Request` - Old password incorrect or validation failed.

**Roles Allowed:** Authenticated users

---

### POST `/auth/forgot-password`
Initiate password reset via email.

**Request Body:**

```json
{
  "email": "string"
}
```

**Responses:**
- `200 OK` - Password reset link sent (even if email not found, to prevent user enumeration).

**Roles Allowed:** Public

---

### POST `/auth/reset-password`
Complete password reset with token.

**Request Body:**

```json
{
  "resetToken": "string",
  "newPassword": "string"
}
```

**Responses:**
- `200 OK` - Password reset successful.
- `400 Bad Request` - Invalid or expired token.

**Roles Allowed:** Public

---

## Profile Management

### GET `/users/me`
Get current logged-in user's profile.

**Responses:**
- `200 OK`

```json
{
  "id": "uuid",
  "username": "string",
  "email": "string",
  "fullName": "string",
  "roles": ["Customer"]
}
```

**Roles Allowed:** Authenticated users

---

### PUT `/users/me`
Update current user's profile.

**Request Body:**

```json
{
  "fullName": "string",
  "email": "string"
}
```

**Responses:**
- `200 OK` - Profile updated.
- `400 Bad Request` - Validation error.

**Roles Allowed:** Authenticated users

---

## Branch Management (Admin Only)

### GET `/branches`
List all branches.

**Query Parameters:**
- `page` (int, optional, default=1)
- `limit` (int, optional, default=20)

**Responses:**
- `200 OK` - List of branches.

**Roles Allowed:** Admin

---

### POST `/branches`
Create a new branch.

**Request Body:**

```json
{
  "name": "string",
  "address": "string",
  "contactNumber": "string"
}
```

**Responses:**
- `201 Created` - Branch created.
- `400 Bad Request` - Validation errors.

**Roles Allowed:** Admin

---

### GET `/branches/{id}`
Get branch details by ID.

**Responses:**
- `200 OK` - Branch details.
- `404 Not Found` - Branch not found.

**Roles Allowed:** Admin

---

### PUT `/branches/{id}`
Update branch information.

**Request Body:** (Fields to update)

```json
{
  "name": "string",
  "address": "string",
  "contactNumber": "string"
}
```

**Responses:**
- `200 OK` - Branch updated.
- `404 Not Found` - Branch not found.

**Roles Allowed:** Admin

---

### DELETE `/branches/{id}`
Delete a branch.

**Responses:**
- `204 No Content` - Branch deleted.
- `404 Not Found` - Branch not found.

**Roles Allowed:** Admin

---

## Account Management

### GET `/accounts`
List all accounts.

**Query Parameters:**
- `page` (int, optional)
- `limit` (int, optional)
- `accountType` (string, optional)
- `status` (string, optional)

**Responses:**
- `200 OK` - List of accounts.

**Roles Allowed:** Admin, Teller

---

### POST `/accounts`
Open a new account.

**Request Body:**

```json
{
  "customerId": "uuid",
  "accountType": "string",
  "initialDeposit": "number"
}
```

**Responses:**
- `201 Created` - Account opened.
- `400 Bad Request` - Validation errors.

**Roles Allowed:** Customer (self), Teller

---

### GET `/accounts/{id}`
View account details.

**Responses:**
- `200 OK` - Account details.
- `404 Not Found` - Account not found.

**Roles Allowed:** Admin, Teller, Owner (Customer)

---

### PUT `/accounts/{id}`
Update account info.

**Request Body:** (e.g., account type)

```json
{
  "accountType": "string"
}
```

**Responses:**
- `200 OK` - Account updated.
- `404 Not Found` - Account not found.

**Roles Allowed:** Admin, Teller

---

### DELETE `/accounts/{id}`
Close account.

**Responses:**
- `204 No Content` - Account closed.
- `404 Not Found` - Account not found.

**Roles Allowed:** Admin, Teller

---

### GET `/accounts/my`
List accounts owned by logged-in customer.

**Responses:**
- `200 OK` - List of accounts.

**Roles Allowed:** Customer

---

## Transactions

### POST `/transactions/deposit`
Deposit money into an account.

**Request Body:**

```json
{
  "accountId": "uuid",
  "amount": "number"
}
```

**Responses:**
- `201 Created` - Deposit successful.
- `400 Bad Request` - Validation error.

**Roles Allowed:** Customer (self), Teller, Admin

---

### POST `/transactions/withdraw`
Withdraw money from an account.

**Request Body:**

```json
{
  "accountId": "uuid",
  "amount": "number"
}
```

**Responses:**
- `201 Created` - Withdrawal successful.
- `400 Bad Request` - Insufficient funds or validation error.

**Roles Allowed:** Customer (self), Teller, Admin

---

### POST `/transactions/transfer`
Transfer funds between accounts.

**Request Body:**

```json
{
  "fromAccountId": "uuid",
  "toAccountId": "uuid",
  "amount": "number"
}
```

**Responses:**
- `201 Created` - Transfer successful.
- `400 Bad Request` - Validation error or insufficient funds.

**Roles Allowed:** Customer (self), Teller, Admin

---

### GET `/transactions`
List all transactions.

**Query Parameters:**
- `page` (int, optional)
- `limit` (int, optional)
- `accountId` (uuid, optional)
- `dateFrom` (date, optional)
- `dateTo` (date, optional)

**Responses:**
- `200 OK` - List of transactions.

**Roles Allowed:** Admin, Teller

---

### GET `/transactions/my`
View logged-in user's transactions.

**Query Parameters:** Pagination supported.

**Responses:**
- `200 OK` - List of user’s transactions.

**Roles Allowed:** Customer

---

### GET `/transactions/{id}`
Get transaction details.

**Responses:**
- `200 OK` - Transaction info.
- `404 Not Found` - Transaction not found.

**Roles Allowed:** Admin, Teller, Owner (Customer)

---

## Reports (Manager/Teller)

### GET `/reports/daily-summary`
Get daily transaction summary.

**Query Parameters:**
- `date` (date, optional, defaults to today)

**Responses:**
- `200 OK` - Summary data.

**Roles Allowed:** Manager, Teller

---

### GET `/reports/account/{id}`
Get report for specific account.

**Responses:**
- `200 OK` - Account report.

**Roles Allowed:** Manager, Teller

---

### GET `/reports/customer/{id}`
Get report for specific customer.

**Responses:**
- `200 OK` - Customer report.

**Roles Allowed:** Manager, Teller

---

## Customer Management (Teller/Admin)

### GET `/customers`
List all customers.

**Query Parameters:** Pagination and filters.

**Responses:**
- `200 OK` - List of customers.

**Roles Allowed:** Teller, Admin

---

### GET `/customers/{id}`
View customer profile.

**Responses:**
- `200 OK` - Customer info.

**Roles Allowed:** Teller, Admin

---

### PUT `/customers/{id}`
Update customer profile.

**Request Body:** Partial or full customer data.

**Responses:**
- `200 OK` - Profile updated.

**Roles Allowed:** Teller, Admin

---

### DELETE `/customers/{id}`
Delete customer.

**Responses:**
- `204 No Content` - Customer deleted.

**Roles Allowed:** Admin

---

## Teller Management (Manager Only)

### GET `/tellers`
List all tellers.

**Responses:**
- `200 OK`

**Roles Allowed:** Manager

---

### POST `/tellers`
Create new teller.

**Request Body:**

```json
{
  "username": "string",
  "email": "string",
  "password": "string",
  "fullName": "string"
}
```

**Responses:**
- `201 Created` - Teller created.

**Roles Allowed:** Manager

---

### GET `/tellers/{id}`
View teller info.

**Responses:**
- `200 OK`

**Roles Allowed:** Manager

---

### PUT `/tellers/{id}`
Update teller details.

**Request Body:** Fields to update.

**Responses:**
- `200 OK`

**Roles Allowed:** Manager

---

### DELETE `/tellers/{id}`
Remove teller.

**Responses:**
- `204 No Content`

**Roles Allowed:** Manager

---

## Loan Management

### POST `/loans/apply`
Apply for a loan.

**Request Body:**

```json
{
  "amount": "number",
  "loanType": "string",
  "termMonths": "int",
  "purpose": "string"
}
```

**Responses:**
- `201 Created` - Loan application submitted.
- `400 Bad Request` - Validation error.

**Roles Allowed:** Customer

---

### GET `/loans/my`
View logged-in user’s loans.

**Responses:**
- `200 OK`

**Roles Allowed:** Customer

---

### GET `/loans`
View all loans.

**Query Parameters:** Pagination supported.

**Responses:**
- `200 OK`

**Roles Allowed:** Teller, Admin

---

### GET `/loans/{id}`
View specific loan.

**Responses:**
- `200 OK`

**Roles Allowed:** Teller, Admin, Owner (Customer)

---

### PUT `/loans/{id}`
Update loan info.

**Request Body:** Fields to update.

**Responses:**
- `200 OK`

**Roles Allowed:** Teller, Admin

---

### POST `/loans/{id}/repay`
Repay loan amount.

**Request Body:**

```json
{
  "amount": "number"
}
```

**Responses:**
- `201 Created` - Repayment successful.

**Roles Allowed:** Customer

---

### POST `/loans/{id}/approve`
Approve a loan.

**Responses:**
- `200 OK` - Loan approved.

**Roles Allowed:** Manager

---

## AI & Analytics (System/Internal Use)

### GET `/analytics/fraud-flags`
List flagged suspicious activities.

**Responses:**
- `200 OK`

**Roles Allowed:** System/Admin

---

### GET `/analytics/credit-score`
Get AI-calculated credit score.

**Query Params:**
- `customerId` (uuid, required)

**Responses:**
- `200 OK`

**Roles Allowed:** System/Admin

---

### GET `/analytics/loan-risk`
AI-based loan risk evaluation.

**Query Params:**
- `loanId` (uuid, required)

**Responses:**
- `200 OK`

**Roles Allowed:** System/Admin

---

### GET `/analytics/usage`
General system usage stats.

**Responses:**
- `200 OK`

**Roles Allowed:** System/Admin

---

### GET `/analytics/personalized`
Get tailored recommendations.

**Query Params:**
- `customerId` (uuid, required)

**Responses:**
- `200 OK`

**Roles Allowed:** System/Admin

---

## Role & Access Management (Admin Only)

### GET `/roles`
List roles.

**Responses:**
- `200 OK`

---

### POST `/roles`
Create role.

**Request Body:** Role details.

**Responses:**
- `201 Created`

---

### PUT `/roles/{id}`
Update role.

**Responses:**
- `200 OK`

---

### DELETE `/roles/{id}`
Delete role.

**Responses:**
- `204 No Content`

---

### GET `/access-control`
List platform access rules.

**Responses:**
- `200 OK`

---

### PUT `/access-control`
Update access rules.

**Responses:**
- `200 OK`

---

## Security & System Management (Admin Only)

### GET `/system/logs`
View system logs.

**Responses:**
- `200 OK`

---

### GET `/system/status`
Get system status.

**Responses:**
- `200 OK`

---

### POST `/system/backup`
Trigger system backup.

**Responses:**
- `202 Accepted`

---

### POST `/system/restore`
Restore from backup.

**Responses:**
- `202 Accepted`

---

### GET `/system/policies`
View security policies.

**Responses:**
- `200 OK`

---

### PUT `/system/policies`
Update security settings.

**Responses:**
- `200 OK`

---

## AI/Chat Support

### GET `/chatbot/ask`
Chatbot query for customer help.

**Query Parameters:**
- `question` (string, required)

**Responses:**
- `200 OK` - Chatbot response.

---

## Scheduled Transactions (Planned Phase 2)

### POST `/schedule/transaction`
Create future-dated transaction.

**Request Body:** Transaction details with future date.

**Responses:**
- `201 Created`

---

### GET `/schedule/transactions`
View scheduled transactions.

**Responses:**
- `200 OK`

---

## Feedback

### POST `/feedback`
Submit feedback.

**Request Body:**

```json
{
  "subject": "string",
  "message": "string"
}
```

**Responses:**
- `201 Created`

---

### GET `/feedback`
View all feedback.

**Responses:**
- `200 OK`

**Roles Allowed:** Admin

---

## Utility Endpoints

### GET `/enums/account-types`
Get list of supported account types.

---

### GET `/enums/loan-types`
Get list of loan types.

---

### GET `/enums/transaction-types`
Get supported transaction types.

---

## API Documentation

- Swagger/OpenAPI spec available at `/api-docs`
- Supports `application/json`
- OAuth2/JWT Bearer token authentication

---

## Error Response Format

All errors follow this JSON structure:

```json
{
  "error": "ErrorCode",
  "message": "Detailed error message"
}
```

---

## Versioning & Deprecation

- API is versioned with base URL `/api/v1/`
- Future breaking changes will be released under `/api/v2/`
- Deprecated endpoints will include warnings in responses

---

## Health Check

### GET `/health`
Simple API health check.

**Responses:**
- `200 OK` - API running fine.

---

# End of Document
