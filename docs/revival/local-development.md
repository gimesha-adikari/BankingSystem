# Backend local development

This guide applies to the standalone `gimesha-adikari/BankingSystem` Spring
Boot repository. The web client, AI/KYC service, and Android application are
independent repositories and are not required as sibling directories.

## Local dependencies

| Dependency | Default local address |
| --- | --- |
| MySQL 8.4 | `127.0.0.1:3307` |
| SMTP sink | `127.0.0.1:1025` |
| FastAPI AI/KYC service | `http://127.0.0.1:8000` |
| Spring Boot backend | `http://127.0.0.1:8080` |

The backend reaches the AI/KYC service through its configured HTTP URL, usually
`ML_BASE_URL`. It does not assume a local path to the `banking-service` checkout.

## Required versions

- Java 21
- Gradle through the checked-in wrapper
- MySQL 8.4 for the integration suite

## Configuration

```bash
cp src/main/resources/application-dev.example.yml src/main/resources/application-dev.yml
```

Set only local sandbox values. Keep credentials and generated runtime files
ignored. Configure `ML_BASE_URL` to the independently running FastAPI service.

## Start and validate

```bash
# From the BankingSystem repository root.
bash ./gradlew test
bash ./gradlew bootRun --args='--spring.profiles.active=dev'
```

The database scripts under `scripts/db/` are read-only or explicitly guarded
operator tools for Flyway adoption. Run them with a protected credentials file
or environment variables; never put database passwords in command arguments.

## Related repositories

- [bank-web](https://github.com/gimesha-adikari/bank-web) — React/Vite client
- [banking-service](https://github.com/gimesha-adikari/banking-service) — FastAPI AI/KYC service
- [BankApp](https://github.com/gimesha-adikari/BankApp) — Android client
