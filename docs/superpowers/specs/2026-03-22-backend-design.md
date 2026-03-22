# AppDistribution Backend — Design Spec

**Date:** 2026-03-22
**Status:** Approved
**Scope:** Backend MVP — дополнение к существующему плану `docs/superpowers/plans/2026-03-21-backend-mvp.md`

---

## 1. Контекст

Существующая документация:
- `docs/PRD.md` — продуктовые требования
- `docs/ARCHITECTURE.md` — системная архитектура, API surface, data model
- `docs/superpowers/plans/2026-03-21-backend-mvp.md` — план реализации (Tasks 1–12)

Данный документ фиксирует дизайн-решения, выявленные в процессе brainstorming, которые отсутствуют в существующем плане. Результат: **3 новые задачи** (Tasks 13–15) и **точечные правки** в существующих задачах.

---

## 2. Дизайн-решения

### 2.1 Auth Flow + Workspace Auto-Create

**Решение:** workspace создаётся автоматически при первом OTP-входе на основе email-домена.

**DB schema fix:** поле `workspace_id` в `UsersTable` должно быть **nullable** (`UUID?`). Это необходимо, так как Workspace создаётся в той же транзакции, что и User. Порядок операций — сначала Workspace, потом User — гарантирует консистентность. Существующий план (Task 3) должен изменить колонку на nullable.

**Свободные email-домены** (`gmail.com`, `hotmail.com`, `outlook.com`, `yahoo.com` и др.) не используются как workspace-slug. Полный список `FREE_EMAIL_DOMAINS` жёстко задан в коде. Если домен свободный — workspace slug формируется из полного email с заменой `@` на `-` (например `john-at-gmail.com`), что гарантирует изоляцию таких пользователей в отдельных workspace.

**Flow в `AuthService.verifyOtp(email, otp)` (единая транзакция):**

```
1. Проверить OTP-код (TTL 5 мин); удалить запись после использования.
   Неверный/просроченный OTP → 401. НЕ логировать в AuditLog.
2. Определить slug: если домен в FREE_EMAIL_DOMAINS → slug = email.replace("@","-")
                    иначе → slug = домен (e.g. "acme.com")
3. Найти Workspace со slug = slug
   Если не найден → создать Workspace(name=slug, slug=slug, owner=null пока)
4. Найти User по email
   Если не найден → определить роль:
       если Workspace только что создан → роль = Admin
       иначе → роль = Tester
     создать User(email, workspaceId=workspace.id, role=role)
     если Workspace только что создан → установить workspace.ownerId = user.id
5. Выдать JWT { sub=userId, email, role, workspace_id, exp=+1h }
   + RefreshToken(token=UUID, userId, expiresAt=+30d) → сохранить в БД
6. AuditLog: action="user.login", userId=user.id, resourceType="user", resourceId=user.id
```

**Правило ролей:**
- Первый пользователь домена (создаёт workspace) → **Admin**
- Последующие пользователи того же домена → **Tester**

**Refresh token rotation** (уже в существующем плане Task 5): при использовании `POST /auth/refresh` старый токен инвалидируется, выдаётся новый.

**Ошибка stale JWT:** если `workspaceId` из JWT не найден в БД — вернуть `401 Unauthorized` с кодом `WORKSPACE_NOT_FOUND`. Это edge-case: workspace был удалён после выдачи токена.

**Затрагивает:** Task 3 (UsersTable — nullable workspace_id), Task 5 (AuthService — workspace provisioning + audit).

---

### 2.2 RBAC Enforcement

**Решение:** явная проверка через extension-функцию `requireRole()` в каждом route handler. Без централизованного middleware — проверка читаемая и отлаживаемая.

**Реализация:**

```kotlin
// plugins/Authentication.kt
data class AuthPrincipal(
    val userId: String,
    val email: String,
    val role: Role,
    val workspaceId: String
)

// Extension для проверки роли в route handlers
fun ApplicationCall.requireRole(vararg roles: Role) {
    val principal = principal<AuthPrincipal>()
        ?: throw UnauthorizedException()
    if (principal.role !in roles) throw ForbiddenException()
}
```

**RBAC Matrix:**

| Endpoint | Admin | Uploader | Tester | Viewer |
|----------|:-----:|:--------:|:------:|:------:|
| `GET /workspaces` | ✅ | ✅ | ✅ | ✅ |
| `POST /workspaces` | Phase 2 — не реализуется в MVP (workspace создаётся автоматически при OTP-входе) | | | |
| `GET /workspaces/:id/projects` | ✅ | ✅ | ✅ | ✅ |
| `POST /workspaces/:id/projects` | ✅ | ❌ | ❌ | ❌ |
| `GET /projects` | ✅ | ✅ | ✅ | ✅ |
| `DELETE /projects/:id` | ✅ | ❌ | ❌ | ❌ |
| `GET /projects/:id/builds` | ✅ | ✅ | ✅ | ✅ |
| `GET /builds/:id` | ✅ | ✅ | ✅ | ✅ |
| `GET /builds/recent` | ✅ | ✅ | ✅ | ✅ |
| `POST /builds/upload` | ✅ | ✅ | ❌ | ❌ |
| `PATCH /builds/:id` | ✅ | ✅ | ❌ | ❌ |
| `DELETE /builds/:id` | ✅ | ❌ | ❌ | ❌ |
| `GET /builds/:id/download-url` | ✅ | ✅ | ✅ | ❌ |
| `GET /users/me` | ✅ | ✅ | ✅ | ✅ |
| `PATCH /users/me` | ✅ | ✅ | ✅ | ✅ |

**Ошибки:** `401 Unauthorized` — нет/невалидный JWT; `403 Forbidden` — недостаточно прав.

**Workspace isolation:** все запросы к проектам и сборкам автоматически фильтруются по `workspaceId` из JWT — пользователь не может получить доступ к данным чужого workspace даже зная ID.

**Затрагивает:** Task 14 (новая) — добавить `requireRole` + `UnauthorizedException` + `ForbiddenException`; применить ко всем routes из Tasks 6, 10, 11.

---

### 2.3 FCM Push Notifications

**Решение:** Firebase Admin SDK. Уведомления отправляются при загрузке новой сборки.

**Trigger:** `BuildService.uploadBuild()` вызывает `NotificationService.notifyNewBuild()` после успешного сохранения Build в БД.

**Сигнатура:**
```kotlin
suspend fun notifyNewBuild(build: Build, projectName: String, workspaceId: UUID)
```
`projectName` передаётся из `BuildService` (который уже имеет доступ к `Project` при upload).

**Flow:**

```
NotificationService.notifyNewBuild(build, projectName, workspaceId):
  1. Если Firebase не инициализирован → log.warn("FCM disabled") + return
  2. UserRepository.findAllByWorkspace(workspaceId)
       .filter { role in [Admin, Uploader, Tester] && fcmToken != null }
  3. Если токенов нет → return
  4. Firebase.messaging().sendEachForMulticast(
       MulticastMessage.builder()
         .addAllTokens(tokens)
         .putData("new_build", "true")
         .putData("build_id", build.id.toString())
         .setNotification(Notification.builder()
             .setTitle("Новая сборка: ${build.versionName}")
             .setBody("$projectName • ${build.channel}")
             .build())
         .build()
     )
  5. Логировать результат (successCount/failureCount), не бросать исключение
```

**Инициализация (Application.kt):**
```kotlin
val credentialsPath = environment.config.propertyOrNull("firebase.credentialsPath")?.getString()
if (credentialsPath != null) {
    val options = FirebaseOptions.builder()
        .setCredentials(GoogleCredentials.fromStream(FileInputStream(credentialsPath)))
        .build()
    FirebaseApp.initializeApp(options)
    log.info("Firebase initialized")
} else {
    log.warn("FIREBASE_CREDENTIALS_PATH not set — push notifications disabled")
}
```

**Проверка инициализации в NotificationService:**
```kotlin
private val isEnabled get() = FirebaseApp.getApps().isNotEmpty()
```

**Конфигурация:**
```hocon
firebase {
  credentialsPath = ${?FIREBASE_CREDENTIALS_PATH}
}
```

**Важно:** FCM-ошибка не блокирует upload. Вызов из `BuildService` — fire-and-forget:
```kotlin
launch { runCatching { notificationService.notifyNewBuild(build, project.name, workspaceId) } }
```

**Новая задача:** Task 13.

---

### 2.4 Projects Endpoint

**Решение:** два варианта путей сосуществуют.

| Endpoint | Описание |
|----------|----------|
| `GET /api/v1/projects` | Android-клиент — проекты из workspace JWT (без workspaceId в URL) |
| `GET /api/v1/workspaces/{id}/projects` | Явный по workspaceId (для будущего Web) |
| `POST /api/v1/workspaces/{id}/projects` | Создание проекта (Admin) |

`GET /api/v1/projects` — backend читает `workspaceId` из JWT principal. Если workspace не найден → `404 WORKSPACE_NOT_FOUND`.

**Затрагивает:** Task 11 (ProjectRoutes) — добавить `GET /projects` route.

---

### 2.5 Audit Log Writes

**Решение:** пишется из service-слоя явно. `AuditRepository.log()` — fire-and-forget через `launch { runCatching { } }`. Ошибка записи не блокирует основную операцию.

**Что логируется:**

| Событие | action | resource_type | Примечание |
|---------|--------|---------------|-----------|
| Успешный OTP-вход | `user.login` | `user` | Неуспешные попытки НЕ логируются |
| Загрузка сборки | `build.upload` | `build` | |
| Удаление сборки | `build.delete` | `build` | |
| Изменение статуса сборки | `build.update` | `build` | |
| Создание проекта | `project.create` | `project` | |
| Удаление проекта | `project.delete` | `project` | |

**Паттерн:**
```kotlin
launch { runCatching { auditRepo.log(userId, "build.upload", "build", build.id, mapOf("version" to build.versionName)) } }
```

**Затрагивает:** Tasks 5, 9, 11.

---

### 2.6 Вне MVP (Phase 2)

- Install-event / Download-event трекинг
- `GET /api/v1/audit-logs` HTTP-эндпоинт
- Notification inbox (`GET /api/v1/notifications`)
- Invite links, тестерские группы

---

## 3. Изменения в существующем плане

### Правки в существующих задачах

| Task | Изменение |
|------|-----------|
| **Task 3** (Database) | `UsersTable.workspaceId` → nullable (`uuid("workspace_id").references(...).nullable()`) |
| **Task 5** (AuthService) | Добавить workspace auto-create + `FREE_EMAIL_DOMAINS` логику в `verifyOtp`; добавить audit write `user.login` |
| **Task 9** (BuildService) | Добавить вызов `NotificationService.notifyNewBuild()` после upload; добавить audit writes для upload/delete/update |
| **Task 11** (ProjectRoutes) | Добавить `GET /projects` route (workspaceId из JWT); добавить `requireRole` вызовы; добавить audit writes |

### Новые задачи

| Task | Название | Описание |
|------|----------|----------|
| **Task 13** | FCM NotificationService | Firebase Admin SDK init, `NotificationService`, `UserRepository.findAllByWorkspace()` |
| **Task 14** | RBAC | `requireRole()` extension, `ForbiddenException`, `UnauthorizedException`; применить ко всем routes из Tasks 6, 10, 11; workspace isolation фильтрация |
| **Task 15** | Integration tests | Testcontainers (PostgreSQL + MinIO) + `testApplication` end-to-end: auth flow, upload flow, RBAC checks |

---

## 4. Tech Stack (без изменений)

| Компонент | Выбор |
|-----------|-------|
| Runtime | Kotlin 2.1.10, JVM 21 |
| Framework | Ktor 3.x (Netty engine) |
| ORM | Exposed 0.58+ |
| Database | PostgreSQL 16 |
| Object Storage | MinIO |
| Auth | JWT (java-jwt 4.x) + OTP |
| Push | Firebase Admin SDK 9.x |
| APK parsing | net.dongliu:apk-parser 2.6.10 |
| Serialization | kotlinx.serialization 1.7.3 |
| Logging | kotlin-logging + Logback |
| Tests | Ktor testApplication + MockK + Testcontainers |
| Deploy | Docker + docker-compose |
