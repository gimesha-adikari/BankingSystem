# SDD ledger — plan: /home/gimesha/My_Projects/Bank/docs/superpowers/plans/2026-09-30-phase4a-security-hardening.md

## Setup
- Base SHA: ba09931d1ff78bdf483dada4f678b4e6f7a234f9 (revival/reconcile-2026)
- Hardening branch: revival/hardening-2026
- Post-cherry-pick SHA: cf43ee24693d4f145e0915b0828b4225d0353065
- Cherry-pick confirmed: only README.md changed (1 file, 2 +-1)
- Worktree: /home/gimesha/My_Projects/Bank/banking-revival-audit/BankingSystem/hardening

## Pre-flight conflict scan
| Tasks | Shared Interface | Finding |
|---|---|---|
| Task 1 + Task 3 | WalletServiceImpl | Task 1 fixes getPaymentIntent; Task 3 changes createQrPayment/createReload/createBill. Different methods — no conflict. |
| Task 3 + IdempotencyService | IdempotencyService signature | Task 3 adds userId+operation params. WalletServiceImpl callers must all be updated in same commit. |
| Task 4 + Task 5 | SessionRepository | Task 4 uses logout()/createSession(); Task 5 adds deleteByUserUserId(). Different methods — no conflict. |
| Task 5 + AuthService interface | AuthService.revokeAllSessions | Added in Task 5 — must be present before Task 4 commits if Task 4 references it. But Task 4 doesn't need it. |
| Task 6 + SecurityConfig | SecurityConfig.permitAll | One change, one commit. No other task touches SecurityConfig. |
| Task 7 + GlobalExceptionHandler | GlobalExceptionHandler | One change, one commit. No other task touches it. |

Ruling: All conflicts are non-blocking. Tasks proceed sequentially.

## Schema management clarification
Ruling: The project uses JPA ddl-auto: update, NOT Liquibase. Phase 3 report
incorrectly attributed "Liquibase changeset" to the schema. The seeder classes
(BranchSeeder, RoleSeeder) are CommandLineRunner beans, not Liquibase changesets.
Cost if wrong: If Liquibase is somehow also configured, adding entity columns via
JPA ddl-auto would still work; Liquibase manages schema changes separately.
The build.gradle has no liquibase dependency — this is definitive.

## IdempotencyKey PK architecture ruling
Ruling: Do NOT change the @Id of IdempotencyKey entity. Instead, compose the
storage key in WalletServiceImpl as "userId:operation:clientKey". This avoids
any PK migration concern and requires no schema change. The unique constraint
is naturally enforced by the composite string key.
Cost if wrong: Two different users with the same composite string key collision
is UUID-space impossible in practice for userId + operation namespacing.

## Task 0: Setup + Baseline
Status: COMPLETE
- Branch: revival/hardening-2026 @ cf43ee24693d4f145e0915b0828b4225d0353065
- Cherry-pick: README.md only commit 3bcd56f → cf43ee2 confirmed
- Baseline compile: exit 0 ✅
- Baseline tests: 5 pass, 4 fail (pre-existing infra failures: contextLoads + KycMlContractIT)
- Frontend lint: exit 0 ✅, build: exit 0 ✅
- npm install run in hardening worktree frontend (node_modules not present in worktree)
- 01-starting-state.md written

## Tasks 1-8: Security fixes
Status: Dispatched to implementer subagent (1a3b5460-1c19-4ca9-9071-abc12501fa0d)
BASE_BEFORE_IMPLEMENTER: cf43ee24693d4f145e0915b0828b4225d0353065

## Todo
- [x] Task 0: Branch setup
- [ ] Task 0: Baseline tests (running)
- [ ] Task 1: P0-1 Wallet BOLA
- [ ] Task 2: P0-2 Webhook disabled
- [ ] Task 3: P0-3 Idempotency scoping
- [ ] Task 4: P1-1 Refresh session sync
- [ ] Task 5: P1-2+P1-3 Password session revocation
- [ ] Task 6: P1-4 Resend verification accessible
- [ ] Task 7: P1-5 Auth exceptions → 403
- [ ] Task 8: KYC duplicate case verification
- [ ] Task 9: Final verification + reports
