# Banking-system revival reconciliation log

This log belongs to the clean local branch `revival/reconcile-2026`. It is not a
replacement for the preservation snapshot of the original dirty checkout.

## Preservation gate

- Original checkout: `/home/gimesha/My_Projects/multi-lng/multi-platform-banking-system`
- Preservation snapshot: `/home/gimesha/My_Projects/multi-lng/revival-snapshots/20260919-001405`
- Snapshot verification: `snapshot-verification.log` reports matching status,
  patch/archive presence, and SHA-256 verification.
- No reset, clean, pull, merge, rebase, stash, checkout-over-dirty-files,
  commit, or push was performed in the original checkout.

## Controlled sources

| ID | Source | Revision/state | Initial role |
| --- | --- | --- | --- |
| BS-LOCAL | Original local BankingSystem | `4c919843b12d` plus preserved dirty tree | Source of local-only backend/frontend/AI/KYC work; never used as a disposable worktree |
| BS-GITHUB | Clean clone of GitHub BankingSystem | `5afe20e3797191b1f9535185f2caecbe993cdb38` | Initial backend, AI, web and documentation baseline |
| APP-LOCAL | Embedded local `android-app/BankApp` | Preserved in the snapshot; incomplete infrastructure | Feature reference only; not used as the Android baseline |
| APP-GITHUB | Clean clone of GitHub BankApp | `1e59a6b4a780a5ff5c73743c57b195538df7b080` | Canonical Android architecture and initial source baseline |
| APP-LEGACY-IN-BS | BankingSystem historical Android tree | Preserved history archive | Behavioral reference for auth, secure storage, lock/PIN, biometric and profile flows |

## Baseline decisions

| Area | Decision | Evidence and reason |
| --- | --- | --- |
| Repository workspace | KEEP GITHUB as the clean starting point | BankingSystem and BankApp histories diverged; the clean source is reproducible and does not risk the dirty checkout |
| Backend core | KEEP GITHUB, then MANUAL MERGE justified local changes | GitHub commit is the controlled baseline; local changes are evaluated by semantic diff, not timestamps |
| AI/KYC | KEEP GITHUB stable implementation plus PORT selected local behavior independently | Local uncommitted KYC adds rotation, crop heuristics, OCR normalization, address matching and decision checks; whole-file replacement is unsafe |
| Android architecture | KEEP APP-GITHUB | It has complete Gradle/manifest/application infrastructure, Hilt, navigation, auth, accounts, KYC, wallet, payment UI, lock and biometric support |
| Embedded Android | DO NOT USE AS BASELINE; retain for comparison | Its project files include empty application/manifest/Gradle resources, although parts of its source are useful |
| Historical Android | REFERENCE ONLY | It contains useful older behavior but is architecturally superseded; wholesale copying would duplicate or regress features |
| Android forgot-password | MANUAL MERGE / FIX | Client used a query parameter while Spring expects JSON `{email}` |
| Android KYC upload types | KEEP APP-GITHUB uppercase wire values | Backend allow-list is `DOC_FRONT`, `DOC_BACK`, `SELFIE`, `ADDRESS_PROOF`; lowercase local values are rejected |
| React API client | KEEP canonical `src/api/axios.ts`; remove hardcoded duplicate behavior | It already uses `VITE_API_BASE_URL`, token injection and normalized errors; `utils/axiosInstance.tsx` hardcodes localhost |
| Backend configuration | MANUAL MERGE safe templates only | Ignored local config contains required names and private values; templates must preserve names without values |
| Model binaries | PRESERVE OUTSIDE GIT until provenance/release policy is decided | Local models are valuable but large and absent from the GitHub baseline; checksums and expected paths are documented separately |
| Database migrations | INVESTIGATE / DOCUMENT, do not introduce a migration framework | Current project uses Hibernate/JPA configuration; adding Flyway/Liquibase would exceed revival scope |
| Wallet/PayHere | KEEP implementation; sandbox/fake provider only locally | No real financial transactions or production credentials are in scope |

## Known unresolved items

- `KycCaseController` currently disables all active-case statuses in a switch;
  the correct lifecycle policy needs tests and product confirmation before it is
  changed.
- `/api/v1/auth/resend-verification` now replaces/reuses the user's verification
  token and sends the existing verification template, while unknown/active
  accounts remain generic no-ops. Mail-sink delivery and abuse/rate-limit
  policy still need a dedicated integration test.
- Android account `branchId` and transaction timestamp serialization are fixed
  in the clean baseline: branch IDs use the backend integer contract and
  timestamps remain ISO strings at the wire/UI boundary.
- The branch creation controller is still a stub; the local sandbox used
  existing branch rows and did not treat that endpoint as production-ready.
- KYC active-case transition policy remains unresolved and deliberately
  unchanged.
- The local AI expansion is uncommitted and has optional modules/models that are
  not all present in the GitHub baseline. It must remain independently testable.
- PayHere/email/model credentials are intentionally not copied into the branch.

## Historical/dirty work retained for later review

The snapshot retains the tracked diff, staged diff, working-tree patch,
project-related untracked files, ignored configuration filenames and protected
copies of sensitive configuration. It also retains the embedded Android archive,
historical Android archive, AI model inventory/checksums and model archive.

## Verified local gates so far

- AI service: `27 passed, 1 skipped` with the portable Python environment.
- Spring backend: full `test` task passed once isolated MySQL and AI were
  supplied (`9 tests completed`, 0 failures, 0 errors: application context,
  auth contract, KYC status, and ML contract tests).
- React: `npm ci --ignore-scripts`, `npm run lint`, and `npm run build` passed.
- Local HTTP smoke/E2E: backend startup, auth/token validation/profile,
  forgot-password body validation, account creation/list/get/transactions, four
  typed KYC uploads, KYC submit/caseId/status/checks, AI connectivity, and Vite
  serving passed in the sandbox.
- Android: unit tests and `assembleDebug` passed with the clean GitHub baseline
  plus contract/config fixes. The verified host SDK exposes API 36 extension 20
  and Build Tools 36.1.0; a temporary SDK view mapped those installed packages
  to the package identifiers expected by this Gradle baseline. `lintDebug` was
  started but intentionally stopped during its long analyzer phase without
  claiming a pass.
- AI Ruff: not clean (`54` findings, predominantly pre-existing style/unused
  imports); no broad formatting sweep was performed during revival.
