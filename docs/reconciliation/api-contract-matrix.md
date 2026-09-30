# Client/backend contract matrix

This matrix records contracts verified against controller/DTO source and the
clean reconciliation clients. It is intentionally narrower than a generated
OpenAPI document and should be extended as each client surface is exercised.

| Client | Feature | Client request | Backend contract | Result | Notes |
| --- | --- | --- | --- | --- | --- |
| Android | Login | `POST /api/v1/auth/login`, JSON login DTO | Same path/body; token response | MATCH | GitHub Android baseline retained |
| React | Login | Canonical Axios client, JSON login DTO | Same path/body; token response | MATCH | `VITE_API_BASE_URL` driven |
| Android | Forgot password | `POST /api/v1/auth/forgot-password`, body `{email}` | `@RequestBody ForgotPasswordRequest` | FIXED / MATCH | Replaced query parameter serialization |
| React | Forgot password | Axios JSON body `{email}` | Same JSON body | MATCH | Modal uses canonical client |
| Android | Resend verification | JSON email request | `POST /api/v1/auth/resend-verification` JSON email request | MATCH | Backend now sends/reuses verification token; delivery still needs a mail sink |
| Android | KYC upload | Multipart `type` values `DOC_FRONT`, `DOC_BACK`, `SELFIE`, `ADDRESS_PROOF` | Same allow-list in `KycUploadController` | FIXED / MATCH | Typed `KycUploadType` prevents lowercase drift |
| Android | KYC submit | JSON upload IDs plus `consent` | `POST /kyc/submit` and `KycSubmitRequest` | MATCH | Local sandbox returned `caseId` and `UNDER_REVIEW` for synthetic images |
| Android | KYC status/checks | `GET /kyc/me`, `GET /kyc/{id}/checks` | Same paths, authenticated | MATCH | Local sandbox returned HTTP 200 |
| Android | Account open | JSON `accountType`, decimal `initialDeposit`, integer `branchId` | `AccountRequestDTO.branchId: Integer` | FIXED / MATCH | Local embedded client had treated branch ID as a string |
| Android | Transactions | Response `createdAt` ISO string | Backend `LocalDateTime` formatted `yyyy-MM-dd'T'HH:mm:ss` | FIXED / MATCH | Removed list-of-integers interpretation |
| React | Accounts/branches | Canonical Axios client | `/api/v1/accounts`, `/api/v1/branches` | PARTIAL | Account flow passed in sandbox; branch-create endpoint is still a stub |
| React | Profile | Canonical Axios `GET/PUT /api/v1/users/me` | Same paths, authenticated | MATCH | Username update re-authenticates through canonical client |
| Android/React | Wallet cards | Authenticated `/api/v1/wallet/cards` | Same path/role requirement | SMOKE MATCH | Local sandbox list returned HTTP 200; no provider transaction performed |
| Android/React | PayHere return/deeplink | Sandbox-only provider routes | `/api/v1/wallet/payhere/*` | NOT EXERCISED | Requires provider-specific sandbox configuration |

## Explicitly rejected assumptions

- A newer repository snapshot was not treated as a universal winner.
- The lower-case embedded Android KYC values were not made compatible by
  weakening the backend allow-list; the client was typed to the backend wire
  values.
- No real financial transaction or production provider credential was used.
