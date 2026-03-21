# AppDistribution — Architecture Document

**Version:** 1.0
**Date:** 2026-03-21

---

## 1. Обзор системы

```
┌─────────────────────────────────────────────────────┐
│                  Android Client                      │
│         (Kotlin + Jetpack Compose + MVI)            │
└─────────────────────┬───────────────────────────────┘
                      │ HTTPS + REST API
                      │
┌─────────────────────▼───────────────────────────────┐
│                  Ktor Backend                        │
│            (Kotlin + Ktor 3.x + JWT)                │
│   ┌──────────┐ ┌──────────┐ ┌────────────────────┐  │
│   │Auth API  │ │Build API │ │Upload/Download API │  │
│   └──────────┘ └──────────┘ └────────────────────┘  │
└───────────┬─────────────┬───────────────────────────┘
            │             │
   ┌────────▼──┐   ┌──────▼──────┐
   │PostgreSQL │   │  MinIO      │
   │(metadata) │   │(APK files)  │
   └───────────┘   └─────────────┘
```

---

## 2. Android Architecture

### Паттерн: MVI (Model-View-Intent)
Выбор обоснован:
- Однонаправленный поток данных → предсказуемое состояние UI
- Хорошо работает с Jetpack Compose и State/Flow
- Легко тестировать ViewModel/Intent обработку изолированно
- Явная типизация всех состояний и действий

### Слои
```
UI Layer (Composables)
    ↕ UiState / UiAction
ViewModel (State + Intent handling)
    ↕ Domain Result
Use Case (business logic)
    ↕ Domain Model
Repository (abstraction over data sources)
    ↕
┌───────────────┬─────────────────┐
│  Remote (API) │  Local (Room)   │
└───────────────┴─────────────────┘
```

### Модульная структура
```
android/
├── app/                        # Entry point, DI graph root
├── core/
│   ├── network/                # ApiService, DTOs, interceptors
│   ├── database/               # Room DB, DAOs, entities
│   ├── common/                 # Result, extensions, utils
│   └── ui/                     # Theme, shared composables
└── feature/
    ├── auth/                   # Login, OTP verification
    ├── home/                   # Feed, recent builds
    ├── projects/               # Project list/detail
    ├── builds/                 # Build list with filters
    ├── build-detail/           # Build card, download, install
    ├── upload/                 # Upload APK
    └── settings/               # Profile, notifications, history
```

**Почему multi-module:**
- Параллельная компиляция → быстрее build
- Явные зависимости → нет случайных "god imports"
- Независимые feature модули → можно передать другому разработчику
- Изоляция тестов

---

## 3. Backend Architecture

### Стек: Ktor 3.x
Выбор перед Spring Boot:
- Lightweight, coroutine-native (нет servlet blocking)
- Kotlin DSL из коробки
- Быстрый старт, меньше boilerplate
- Отличная поддержка multipart upload
- Единый язык с Android → переиспользование моделей

### Структура
```
backend/
├── src/main/kotlin/com/appdist/
│   ├── Application.kt              # Ktor entry point
│   ├── config/                     # AppConfig, DB config
│   ├── plugins/                    # Ktor plugins (auth, routing, etc.)
│   ├── domain/
│   │   ├── model/                  # Domain entities
│   │   ├── repository/             # Repository interfaces
│   │   └── service/                # Business logic
│   ├── api/
│   │   ├── routes/                 # Route definitions
│   │   └── dto/                    # Request/Response DTOs
│   └── infrastructure/
│       ├── database/               # Exposed ORM, migrations
│       ├── storage/                # MinIO client
│       └── apk/                    # APK metadata extractor
```

---

## 4. Data Model

### Core Entities

```
Workspace
├── id: UUID
├── name: String
├── slug: String (unique)
└── owner_id: UUID → User

User
├── id: UUID
├── email: String (unique)
├── name: String
├── role: Enum(admin, uploader, tester, viewer)
├── workspace_id: UUID → Workspace
├── fcm_token: String?
└── created_at: Timestamp

Project
├── id: UUID
├── workspace_id: UUID → Workspace
├── name: String
├── package_name: String
├── icon_url: String?
└── created_at: Timestamp

Build
├── id: UUID
├── project_id: UUID → Project
├── version_name: String          # "1.2.3"
├── version_code: Long            # 12300
├── build_number: String?         # "build-456"
├── flavor: String?               # "staging", "prod"
├── build_type: String            # "debug", "release"
├── environment: Enum(dev,qa,staging,prod_like)
├── channel: Enum(nightly,alpha,beta,rc,internal,custom)
├── branch: String?
├── commit_hash: String?
├── uploader_id: UUID → User
├── upload_date: Timestamp
├── changelog: String?
├── file_size: Long               # bytes
├── checksum_sha256: String
├── min_sdk: Int
├── target_sdk: Int
├── cert_fingerprint: String?
├── abis: List<String>?           # ["arm64-v8a", "x86_64"]
├── storage_key: String           # path in MinIO
├── status: Enum(active,deprecated,archived,mandatory)
├── expiry_date: Timestamp?
└── is_latest_in_channel: Boolean

InstallEvent
├── id: UUID
├── user_id: UUID → User
├── build_id: UUID → Build
├── installed_at: Timestamp
└── device_info: JSON             # model, android_version, etc.

DownloadEvent
├── id: UUID
├── user_id: UUID → User
├── build_id: UUID → Build
└── downloaded_at: Timestamp

AuditLog
├── id: UUID
├── user_id: UUID → User
├── action: String                # "build.upload", "build.delete", etc.
├── resource_type: String
├── resource_id: UUID?
├── metadata: JSON?
└── created_at: Timestamp

InviteLink (Phase 2)
├── id: UUID
├── project_id: UUID → Project
├── token: String (unique)
├── role: Enum
├── created_by: UUID → User
├── expires_at: Timestamp?
├── max_uses: Int?
└── uses: Int
```

---

## 5. API Surface

### Auth
```
POST   /api/v1/auth/request-otp    { email }
POST   /api/v1/auth/verify-otp     { email, otp } → { access_token, refresh_token }
POST   /api/v1/auth/refresh        { refresh_token } → { access_token }
POST   /api/v1/auth/logout
```

### Workspaces & Projects
```
GET    /api/v1/workspaces
POST   /api/v1/workspaces
GET    /api/v1/workspaces/:id/projects
POST   /api/v1/workspaces/:id/projects
GET    /api/v1/projects/:id
PATCH  /api/v1/projects/:id
DELETE /api/v1/projects/:id
```

### Builds
```
GET    /api/v1/projects/:id/builds     ?channel=&env=&search=&page=&limit=
POST   /api/v1/builds/upload           multipart/form-data
GET    /api/v1/builds/:id
PATCH  /api/v1/builds/:id              { changelog, status, expiry_date }
DELETE /api/v1/builds/:id
GET    /api/v1/builds/:id/download-url → { url: signed_url, expires_at }
GET    /api/v1/builds/recent           ?limit=20
```

### Events & Audit
```
POST   /api/v1/builds/:id/install-event   { device_info }
POST   /api/v1/builds/:id/download-event
GET    /api/v1/projects/:id/analytics     (Admin/Uploader)
GET    /api/v1/audit-logs                  ?resource_type=&page=
```

### Users & Notifications
```
GET    /api/v1/users/me
PATCH  /api/v1/users/me             { name, fcm_token }
GET    /api/v1/notifications
PATCH  /api/v1/notifications/:id/read
POST   /api/v1/notifications/read-all
```

---

## 6. Storage Model

### MinIO Bucket Structure
```
appdist-builds/
└── {workspace_id}/
    └── {project_id}/
        └── {build_id}/
            └── app.apk              # actual APK
```

**Signed URL flow:**
1. Client requests `GET /builds/:id/download-url`
2. Backend validates permission
3. Backend generates pre-signed S3/MinIO URL (TTL: 15 min)
4. Client downloads directly from MinIO
5. Client logs download event

---

## 7. Auth Model

```
Email → OTP (6 digits, 5 min TTL) → JWT Access Token (1h) + Refresh Token (30d)

Access Token payload:
{
  sub: userId,
  email: email,
  role: role,
  workspace_id: workspaceId,
  iat, exp
}
```

**Refresh Token:** хранится в БД, позволяет ротацию при компрометации.

---

## 8. Security Considerations

- Signed URLs для APK (15 мин TTL) — APK не раздаётся напрямую, нельзя поделиться прямой ссылкой
- Checksum verification на клиенте после скачивания
- Role-based access на уровне каждого endpoint
- Refresh token rotation (при использовании старый инвалидируется)
- APK signature fingerprint сохраняется — предупреждение о mismatch при установке
- No direct file serving без авторизации
- HTTPS everywhere (TLS termination на reverse proxy)

---

## 9. Архитектурные решения и компромиссы

| Решение | Альтернатива | Обоснование |
|---------|--------------|-------------|
| Ktor вместо Spring Boot | Spring Boot | Kotlin-native, легче, coroutine-first |
| Exposed ORM вместо Hibernate | Hibernate/JPA | Kotlin DSL, нет проблем с lazy loading |
| MinIO вместо cloud S3 | AWS S3 / GCS | Self-hosted, no vendor lock-in |
| OTP вместо Password auth | Password + bcrypt | Проще UX для тестировщиков, нет проблем с паролями |
| MVI вместо MVVM | MVVM | Предсказуемый state, лучше для сложных экранов |
| WorkManager для загрузки | Service / coroutine | Гарантированное выполнение, retry, battery-aware |
| multi-module | monolith | Изоляция, параллельная компиляция |

---

## 10. Масштабирование (future)

- **Multi-tenant:** добавить tenant_id в все таблицы, routing по subdomain
- **Web admin:** API уже готов, добавить React/Next.js frontend
- **AAB support:** расширить Build модель, добавить artifact_type
- **Crash reporting:** добавить модуль Crash с символами и маппингом
- **Horizontal scaling:** Ktor stateless + PostgreSQL + MinIO все масштабируются независимо
