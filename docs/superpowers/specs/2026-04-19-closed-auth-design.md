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

### Removed

- Table: `otps`
- Endpoints: `POST /auth/request-otp`, `POST /auth/verify-otp`
- Logic: auto-create user on OTP verify, auto-assign workspace by email domain

### New Endpoints

**Login**
```
POST /auth/login
Auth: none
Body: { email: String, password: String }
Response 200: { access_token, refresh_token }
Response 401: { code: "INVALID_CREDENTIALS" }
```

Password verified with BCrypt. JWT issued with same claims as before (`userId`, `email`, `role`, `workspaceId`).

**Create user (admin only)**
```
POST /api/v1/workspaces/{workspaceId}/users
Auth: Bearer (ADMIN)
Body: { email: String, name: String, role: "UPLOADER"|"TESTER"|"VIEWER" }
Response 201: { user: UserResponse, generated_password: String }
```

Generates 12-character secure random alphanumeric password, hashes with BCrypt, stores hash. Plain password returned once only.

**Reset password (admin only)**
```
POST /api/v1/workspaces/{workspaceId}/users/{userId}/reset-password
Auth: Bearer (ADMIN)
Body: (empty)
Response 200: { generated_password: String }
```

Generates and stores new password. Plain password returned once only.

### Bootstrap

On application startup, if `ADMIN_EMAIL` and `ADMIN_PASSWORD` env vars are set and no user with that email exists, create the admin user with `ADMIN` role. If user already exists, skip silently.

### Password Generation

12-character string from charset `[A-Z][a-z][0-9]` using `SecureRandom`. BCrypt cost factor: 12.

---

## Android (`feature/auth`)

### Removed

- `OtpScreen.kt`
- `OtpViewModel.kt`
- `RequestOtpUseCase.kt`
- `VerifyOtpUseCase.kt`
- `RequestOtpRequest` data class
- `VerifyOtpRequest` data class
- `ApiService.requestOtp()` and `ApiService.verifyOtp()`

### Changed

**`LoginScreen.kt`** — replace single email field + "get code" button with:
```
[ Email                    ]
[ Password  👁             ]  ← toggle visibility
[ Войти                    ]
```

**`LoginViewModel.kt`** — state holds `email: String` + `password: String` + `isLoading` + `error`. Single `login()` action.

**`LoginUseCase.kt`** — validates email and non-empty password, calls `repository.login(email, password)`, stores tokens via `TokenManager`.

**`ApiService.kt`** — add:
```kotlin
@POST("auth/login")
suspend fun login(@Body request: LoginRequest): Response<TokenResponse>
```

**`LoginRequest.kt`**:
```kotlin
@Serializable
data class LoginRequest(val email: String, val password: String)
```

---

## Web Admin Panel (`admin/`)

### Login Page (`app/login/page.tsx`)

Replace two-step OTP form with a single form:
```
[ Email    ]
[ Password ]
[ Sign In  ]
```

Calls `api.auth.login(email, password)` → saves tokens → redirects to `/overview`.

### Team Page (`app/(dashboard)/team/page.tsx`)

Add for ADMIN role:
- **"Add member" button** in page header → opens `CreateUserDialog`
- **"Reset password" icon button** in each table row → opens `PasswordResultDialog`

**`CreateUserDialog`** fields: email, name, role (dropdown: UPLOADER / TESTER / VIEWER). On submit calls `api.workspace.createUser(workspaceId, payload)`. On success opens `PasswordResultDialog`.

**`PasswordResultDialog`** — displays generated password with copy button. Password shown once; closing the dialog discards it.

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
workspace.createUser(workspaceId, { email, name, role }): Promise<{ user: User, generatedPassword: string }>
workspace.resetPassword(workspaceId, userId): Promise<{ generatedPassword: string }>
```

---

## Out of Scope

- Password strength requirements (admin-generated passwords are always random)
- User self-service password change
- Email notifications on account creation
- Audit log entries for password operations
