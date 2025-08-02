# Use Cases for Multi-Platform Banking System

This document outlines the primary use cases for each role in the banking system, applicable across Web, Android, and Desktop platforms.

---

## Customer Use Cases

| Use Case                                   | Description                                       | Platforms     |
|--------------------------------------------|-------------------------------------------------|---------------|
| Register Account                           | Register with personal details to access services | Web / Android |
| Login to System                            | Secure login using credentials                    | All           |
| View Account Balance & Transaction History | Check balances and view past transactions         | All           |
| Transfer Funds                             | Send money to another account                     | All           |
| Deposit Funds                              | Virtually deposit money into the account          | All           |
| Withdraw Funds                             | Virtually withdraw money from the account         | All           |
| Apply for Loan                             | Submit a loan application                         | Web / Android |
| View Loan Status & History                 | Track loan status and repayment history           | All           |
| Repay Loan                                 | Make loan repayments                              | All           |
| Manage Profile                             | Update profile information                        | All           |
| Receive Notifications                      | Get alerts for transactions and loan updates      | Web / Android |

---

## Teller Use Cases

| Use Case               | Description                                  | Platforms     |
|------------------------|----------------------------------------------|---------------|
| View Customer Profiles | Access customer details for service          | Web / Desktop |
| Initiate Transactions  | Perform deposits, withdrawals, and transfers | Web / Desktop |
| View and Manage Loans  | Assist with loan processing and status       | Web / Desktop |
| Generate Reports       | Create daily transaction summaries           | Desktop       |

---

## Manager Use Cases

| Use Case                      | Description                                | Platforms |
|-------------------------------|--------------------------------------------|-----------|
| Manage Tellers                | Add or remove teller accounts              | Web       |
| Approve High-Value Operations | Authorize large transactions and loans     | Web       |
| Access Analytics              | View usage stats and performance data      | Web       |
| Monitor Fraud/Flags           | Oversee flagged or suspicious transactions | Web       |
| Schedule Maintenance          | Plan downtime or system updates            | Web       |

---

## Admin Use Cases

| Use Case                | Description                                           | Platforms |
|-------------------------|-------------------------------------------------------|-----------|
| Role Management         | Assign roles and permissions to users                 | Web       |
| Platform Access Control | Manage access for each client (Web, Android, Desktop) | Web       |
| System Monitoring       | View logs and system performance                      | Web       |
| Security Enforcement    | Manage policies for encryption, passwords, etc.       | Web       |
| Backup & Recovery       | Handle database backups and system restore            | Web       |

---

## Shared Use Cases

| Use Case        | Description                       | Platforms |
|-----------------|---------------------------------|-----------|
| Logout          | Securely log out of the session   | All       |
| Change Password | Update login credentials securely | All       |

---

## AI/ML Use Cases

| Use Case                               | Description                                                                                                                  | User Role    |
|----------------------------------------|------------------------------------------------------------------------------------------------------------------------------|--------------|
| Fraud Detection                        | Analyze transactions in real-time to identify and flag suspicious or potentially fraudulent activities to protect users.     | System       |
| Credit Scoring                         | Evaluate loan applicants’ creditworthiness using historical and behavioral data to support informed loan approval decisions. | System       |
| Chatbot Support                        | Interact with an AI-powered chatbot for quick responses to common banking queries and assistance with transactions.          | Customer     |
| Personalized Financial Recommendations | Receive tailored advice and offers based on spending habits and financial goals to better manage finances.                   | Customer     |
| Risk Assessment for Loans              | Generate AI-based risk scores for loan applications to prioritize reviews and reduce default rates.                          | Loan Officer |

---

## Future Use Cases (Phase 2)

| Use Case                  | Description                                    | Platforms     |
|---------------------------|------------------------------------------------|---------------|
| Two-Factor Authentication | Extra login security using OTP/email/sms       | Web / Android |
| Chat Support              | In-app support chat with staff                 | Web / Android |
| Voice Banking             | Voice command-based transactions               | Android       |
| AI Fraud Detection        | Detect unusual activity using machine learning | Backend       |
| Customer Feedback         | Collect feedback after interactions            | Web / Android |
| Scheduled Transactions    | Set up future-dated payments                   | All           |
| Investment Services       | Manage savings, investments, and returns       | Web           |
| Dark Mode                 | Switch between light and dark themes           | All           |
