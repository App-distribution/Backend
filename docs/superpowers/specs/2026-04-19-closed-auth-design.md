# Closed Authentication — Design Spec

**Date:** 2026-04-19  
**Branch:** feature/android-client-mvp

---

## Overview

Replace the current open OTP-based authentication with a closed email + password system. Only administrators can create user accounts; users receive generated credentials. Password management is admin-only.

---

## Requirements

- Login: email + password (no OTP, no self-registration)
- User creation: admin-only, system generates a random password shown once
- Password reset: admin-only, new generated password shown once
- Users cannot change their own password
- First admin bootstrapped via environment variables

---

## Backend

### Data Model

Add to `users` table:

```sql
password_hash TEXT NOT NULL DEFAULT ''
```

Update `UserRepository` interface — `create()` must accept `passwordHash: String`:

```kotlin
fun create(workspaceId: UUID, email: String, name: String, role: UserRole, passwordHash: String): User
```

All callers of `create()` (currently in `AuthService`) must be updated. The `DEFAULT ''` sentinel is only for the migration; no code path should ever write an empty hash.

### Removed

- Table: `otps`
- Endpoints: `POST /api/v1/auth/request-otp`, `POST /api/v1/auth/verify-otp`
- Logic: auto-create user on OTP verify, auto-assign workspace by email domain
- `OtpRepository` interface + `OtpRepositoryImpl`
- `AuthService` OTP dependencies (constructor parameter `otpRepository` removed, `sendOtp`/`verifyOtp` methods removed)
- `Routing.kt`: remove `OtpRepositoryImpl` instantiation, remove `authRoutes(authService)` references to OTP handlers
- `AppConfig`: remove `OtpConfig` data class and `otp` key from `application.conf`
- `application.conf`: remove `otp { ttlMinutes, length }` block

### New Endpoints

Full path prefix: `/api/v1` (consistent with existing routes, per `Routing.kt`).

**Login**
```
POST /api/v1/auth/login
Auth: none
Body:   { email: String, password: String }
200:    { access_token, refresh_token }
400:    { code: "INVALID_REQUEST" }   — missing/malformed fields
401:    { code: "INVALID_CREDENTIALS" }
```

Password verified with BCrypt. JWT issued with same claims as before (`userId`, `email`, `role`, `workspaceId`).

**Create user (admin only)**
```
POST /api/v1/workspaces/{workspaceId}/users
Auth: Bearer (ADMIN)
Body:   { email: String, name: String, role: "UPLOADER"|"TESTER"|"VIEWER" }
201:    { user: UserResponse, generated_password: String }
400:    { code: "INVALID_REQUEST" }   — missing/invalid fields
409:    { code: "USER_ALREADY_EXISTS" }
```

Generates a 12-character secure random alphanumeric password (charset `[A-Z][a-z][0-9]`, ~71 bits of entropy; special characters excluded to avoid copy-paste friction). Hashes with BCrypt (cost factor stored as named constant `BCRYPT_COST = 12`). Plain password returned once only — never stored.

**Reset password (admin only)**
```
POST /api/v1/workspaces/{workspaceId}/users/{userId}/reset-password
Auth: Bearer (ADMIN)
Body:   (empty)
200:    { generated_password: String }
404:    { code: "USER_NOT_FOUND" }
```

Generates and stores a new password hash. Plain password returned once only.

### Bootstrap

On application startup, if `ADMIN_EMAIL` and `ADMIN_PASSWORD` env vars are set and no user with that email exists:

1. Create a workspace (name derived from `ADMIN_EMAIL` domain, or `"default"` for free domains).
2. Create the admin user with `ADMIN` role, assigned to that workspace, with BCrypt hash of `ADMIN_PASSWORD`.

If a user with `ADMIN_EMAIL` already exists, skip silently. The bootstrap admin must have a non-null `workspaceId` so the JWT validator accepts the issued token (see `Authentication.kt` — `workspace_id` claim is required).

---

## Android (`feature/auth`)

### Removed

- `OtpScreen.kt`
- `OtpViewModel.kt`
- `RequestOtpUseCase.kt`
- `VerifyOtpUseCase.kt`
- `RequestOtpRequest.kt` data class
- `VerifyOtpRequest.kt` data class
- `ApiService.requestOtp()` and `ApiService.verifyOtp()`
- `auth/otp/{email}` route in `AppNavGraph.kt`
- `onNavigateToOtp` lambda parameter from `LoginScreen`

### Changed

**`AppNavGraph.kt`** — remove `composable("auth/otp/{email}")` and the `onNavigateToOtp` argument passed to `LoginScreen`. The single `auth/login` composable now covers the full auth flow; on success it emits `NavigateToHome`.

**`LoginScreen.kt`** — full rewrite. Replace email-only field + "get code" button with:
```
[ Email                    ]
[ Password  👁             ]  ← toggle visibility
[ Войти                    ]
```
No `onNavigateToOtp` parameter. Receives `onNavigateToHome: () -> Unit`.

**`LoginViewModel.kt`** — full rewrite:
- `LoginUiState`: fields `email`, `password`, `isLoading`, `error`
- `LoginAction`: `EmailChanged`, `PasswordChanged`, `Submit`
- `LoginEffect`: `NavigateToHome` (replaces `NavigateToOtp`)
- Injects `LoginUseCase` (replaces `RequestOtpUseCase`)

**`AuthRepository.kt`** (interface) — remove `requestOtp` and `verifyOtp`, add:
```kotlin
suspend fun login(email: String, password: String): Result<Unit>
```

**`AuthRepositoryImpl.kt`** — remove OTP method implementations, add `login`: calls `api.login(LoginRequest(email, password))`, stores tokens via `TokenManager`, returns `Result.success(Unit)` or `Result.failure(...)`.

**`LoginUseCase.kt`** — new file in package `com.appdist.feature.auth.domain`. Validates non-empty email and password, calls `repository.login(email, password)`, propagates result.

**`ApiService.kt`** — add:
```kotlin
@POST("api/v1/auth/login")
suspend fun login(@Body request: LoginRequest): Response<TokenResponse>
```

**`LoginRequest.kt`** — new data class:
```kotlin
@Serializable
data class LoginRequest(val email: String, val password: String)
```

---

## Web Admin Panel (`admin/`)

### Login Page (`app/login/page.tsx`)

Full rewrite. Replace two-step OTP form with a single form:
```
[ Email    ]
[ Password ]
[ Sign In  ]
```

Calls `api.auth.login(email, password)` → saves tokens → redirects to `/overview`.

### Team Page (`app/(dashboard)/team/page.tsx`)

Add for ADMIN role:
- **"Add member" button** in page header → opens `CreateUserDialog`
- **"Reset password" icon button** in each table row → calls reset, then opens `PasswordResultDialog`

**`CreateUserDialog`** — form fields: email, name, role dropdown (UPLOADER / TESTER / VIEWER). The form does **not** include a password field; the generated password comes from the backend response. On success, the dialog passes the returned `generatedPassword` to `PasswordResultDialog`.

**`PasswordResultDialog`** — displays the generated password with a copy button. Password shown once; closing the dialog discards it from component state.

### `lib/types.ts`

Add:
```ts
export interface CreateUserPayload {
  email: string;
  name: string;
  role: "UPLOADER" | "TESTER" | "VIEWER";
}

export interface CreateUserResponse {
  user: User;
  generatedPassword: string;  // mapped from backend snake_case `generated_password`
}

export interface ResetPasswordResponse {
  generatedPassword: string;  // mapped from backend `generated_password`
}
```

### `lib/api.ts`

Replace:
```ts
auth.requestOtp(email)
auth.verifyOtp(email, otp)
```
With:
```ts
auth.login(email: string, password: string): Promise<AuthTokens>
```

Add:
```ts
workspace.createUser(
  workspaceId: string,
  payload: CreateUserPayload
): Promise<CreateUserResponse>

workspace.resetPassword(
  workspaceId: string,
  userId: string
): Promise<ResetPasswordResponse>
```

Mapping note: `generated_password` (snake_case from API) → `generatedPassword` (camelCase), consistent with existing field mapping conventions in `lib/api.ts`.

---

## Out of Scope

- Password strength requirements (admin-generated passwords are always random)
- User self-service password change
- Email notifications on account creation
- Audit log entries for password operations
