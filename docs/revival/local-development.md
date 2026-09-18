# Local development baseline

This is the safe local development target for the revival branch. It is not a
production deployment guide.

## Components and local ports

| Component | Directory | Default local address |
| --- | --- | --- |
| MySQL | local sandbox or Docker | `127.0.0.1:3307` |
| AI/KYC | `ai-service/bank-ai-service` | `http://127.0.0.1:8000` |
| Spring backend | `backend/corebank` | `http://127.0.0.1:8080` |
| React/Vite | `web-frontend/my-bank-ui` | `http://127.0.0.1:5173` |
| Android | `android-app/BankApp` | Configure `BuildConfig.API_BASE_URL` for the device/emulator |

## Required versions

- Java 21, Gradle wrapper 8.14.3 and Spring Boot 3.4.8.
- Python 3.11 is the intended AI runtime. The checked-in `environment.yml`
  records the tested Conda-family versions; `requirements.txt` now delegates to
  the portable pip-compatible definition for environments without Conda.
- Node/npm versions should be recorded by the developer running the frontend;
  install from the committed `package-lock.json` with `npm ci`.
- Android uses the versions in `android-app/BankApp/gradle/libs.versions.toml`.
  The baseline targets Android API 36 with extension level 20 and explicitly
  uses Build Tools 36.1.0. Install the matching Android SDK platform and build
  tools with Android Studio or `sdkmanager` before running Gradle.

## Configuration

1. Copy `backend/corebank/src/main/resources/application-dev.example.yml` to
   the ignored `application-dev.yml` and set only local sandbox values.
2. Copy `ai-service/bank-ai-service/.env.example` to `.env` and choose simple
   or explicitly available feature backends. Do not put production credentials
   in either file.
3. Keep model files outside Git and point the AI `.env` model paths at the local
   model directory. `models/README.md` documents the preserved local inventory.
4. Set the Android API URL to the backend address reachable from the selected
   emulator/device. `10.0.2.2` is normally used by the Android emulator for the
   host machine; a physical device needs the host LAN address and cleartext
   development access.

## Start order

```text
1. Start an isolated development MySQL database.
2. Start the AI service and confirm GET /health and GET /api/v1/kyc/ping.
3. Start Spring with the dev profile and confirm its actuator/health endpoint.
4. Start Vite with VITE_API_BASE_URL=http://127.0.0.1:8080.
5. Build/install the Android debug APK against the reachable backend.
```

For a disposable local MySQL instance without Docker, initialize a separate
datadir and bind it to port `3307`; do not point this workflow at a personal or
production database. Docker Compose is optional and is not required by the
revival baseline.

## Validation commands

```bash
cd ai-service/bank-ai-service
python -m pytest -q
python -m uvicorn app.main:app --host 127.0.0.1 --port 8000

cd ../../../backend/corebank
# Supply SPRING_DATASOURCE_* and ML_BASE_URL from your local environment.
bash ./gradlew test
bash ./gradlew bootRun --args='--spring.profiles.active=dev'

cd ../../web-frontend/my-bank-ui
npm ci
npm run lint
npm run build
VITE_API_BASE_URL=http://127.0.0.1:8080 npm run dev -- --host 127.0.0.1

cd ../../android-app/BankApp
ANDROID_HOME=/path/to/android-sdk ANDROID_SDK_ROOT=/path/to/android-sdk \\
  bash ./gradlew test -PAPI_BASE_URL=http://10.0.2.2:8080/
ANDROID_HOME=/path/to/android-sdk ANDROID_SDK_ROOT=/path/to/android-sdk \\
  bash ./gradlew assembleDebug -PAPI_BASE_URL=http://10.0.2.2:8080/
```

The KYC integration test in the backend is an opt-in live contract test and
requires the AI service. It is not evidence of a working database or email
configuration by itself. Wallet/PayHere flows remain sandbox-only.

The current security configuration exposes an actuator mapping but may return
HTTP 403 to an unauthenticated `/actuator/health` request; verify startup with
an authenticated request or record the protected-health behavior explicitly.
Registration and verification email flows require a local SMTP sink. Do not
use real mail credentials during development.
