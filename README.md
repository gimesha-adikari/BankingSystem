![Project cover](docs/design/cover.png)

# Banking System
#### Video Demo: (coming soon)

## Description
Multi-Platform Banking System is a full-stack monorepo for retail-banking workflows. It includes a Spring Boot 3 (Java 21) core API with JWT authentication and role-based access, a React + Vite admin/web client, and a FastAPI microservice for KYC/verification. The system models accounts, customers, transactions, loans, and reporting, with clear separation of services and an emphasis on maintainability and production-ready expansion.

---

## Technologies Used
- Java 21, Spring Boot 3, Spring Security (JWT), JPA/Hibernate, MySQL
- TypeScript, React (18/19), Vite, Tailwind CSS
- Python 3, FastAPI, Uvicorn
- OpenAPI/Swagger

---

## How to Run the Project

### 1) Clone the repo
```bash
git clone https://github.com/<your-username>/banking-system.git
cd banking-system
```

### 2) Start the Core API (Spring Boot)
```bash
cd server
./mvnw spring-boot:run
# or: ./gradlew bootRun
# API at http://localhost:8080
```

### 3) Start the Web App (React + Vite)
```bash
cd web
npm install
npm run dev
# Web at http://localhost:5173
```

### 4) Start the AI/KYC Service (FastAPI)
```bash
cd ai-service
python -m venv .venv && source .venv/bin/activate  # Windows: .venv\Scripts\activate
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
# Service at http://localhost:8000
```

> Configure database and service URLs via `.env` files in each service (e.g., DB URL, JWT secret, API base URLs).

---

## Example API Routes
| Route                    | Method | Description                     |
|--------------------------|--------|---------------------------------|
| `/api/auth/login`        | POST   | Issue JWT for authenticated use |
| `/api/customers`         | GET    | List customers                  |
| `/api/accounts`          | GET    | List accounts                   |
| `/api/transactions`      | POST   | Create a transaction            |
| `/api/loans`             | GET    | List or view loans              |
| `/docs` or `/swagger-ui` | GET    | OpenAPI documentation           |

---

## Why This Project?
To provide a pragmatic, end-to-end banking foundation that’s easy to extend: secure core services, a fast modern web client, and a focused AI/KYC microservice for real-world verification flows.

---

## Acknowledgments
- Spring, React, and FastAPI communities
- OpenAPI/Swagger tooling
