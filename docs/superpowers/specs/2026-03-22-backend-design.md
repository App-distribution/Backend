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

**Flow в `AuthService.verifyOtp(email, otp)`:**

```
1. Проверить OTP-код (TTL 5 мин, удалить после использования)
2. Найти пользователя по email
3. Если пользователь не найден → создать User
4. Если у пользователя нет workspace_id:
   a. Извлечь домен: "john@acme.com" → "acme.com"
   b. Найти Workspace со slug = "acme.com"
   c. Если найден → присвоить пользователю, роль = Tester
   d. Если не найден → создать Workspace(name="acme.com", slug="acme.com"),
      назначить пользователя owner, роль = Admin
5. Выдать JWT { sub, email, role, workspace_id, exp=+1h }
   + RefreshToken (TTL 30d, хранится в БД)
```

**Правило ролей при join:**
- Первый пользователь домена → **Admin**
- Последующие пользователи того же домена → **Tester**

**Затрагивает:** Task 5 (AuthService) — добавить workspace provisioning логику.

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

// Расширение для проверки роли в route handlers
fun ApplicationCall.requireRole(vararg roles: Role) {
    val principal = principal<AuthPrincipal>()
        ?: throw UnauthorizedException()
    if (principal.role !in roles) throw ForbiddenException()
}
```

**RBAC Matrix:**

| Endpoint | Admin | Uploader | Tester | Viewer |
|----------|:-----:|:--------:|:------:|:------:|
| `GET /projects` | ✅ | ✅ | ✅ | ✅ |
| `POST /workspaces/:id/projects` | ✅ | ❌ | ❌ | ❌ |
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

**Затрагивает:** Task 14 (новая) — добавить `requireRole` вызовы во все route handlers из Tasks 6, 10, 11.

---

### 2.3 FCM Push Notifications

**Решение:** Firebase Admin SDK. Уведомления отправляются при загрузке новой сборки.

**Trigger:** `BuildService.uploadBuild()` вызывает `NotificationService.notifyNewBuild()` после успешного сохранения Build в БД.

**Flow:**

```
NotificationService.notifyNewBuild(build: Build, workspaceId: UUID):
  1. UserRepository.findAllByWorkspace(workspaceId)
       .filter { role in [Admin, Uploader, Tester] && fcmToken != null }
  2. Если токенов нет → выход
  3. Firebase.messaging().sendEachForMulticast(
       MulticastMessage.builder()
         .addAllTokens(tokens)
         .putData("new_build", "true")
         .putData("build_id", build.id.toString())
         .setNotification(Notification.builder()
             .setTitle("Новая сборка: ${build.versionName}")
             .setBody("${projectName} • ${build.channel}")
             .build())
         .build()
     )
  4. Логировать результат (sent/failed count), не бросать исключение
```

**Конфигурация:**
```hocon
# application.conf
firebase {
  credentialsPath = ${?FIREBASE_CREDENTIALS_PATH}  # путь к service account JSON
}
```

**Инициализация:**
```kotlin
// Application.kt
val credentialsPath = environment.config.propertyOrNull("firebase.credentialsPath")?.getString()
if (credentialsPath != null) {
    val credentials = FileInputStream(credentialsPath)
    val options = FirebaseOptions.builder()
        .setCredentials(GoogleCredentials.fromStream(credentials))
        .build()
    FirebaseApp.initializeApp(options)
}
```

**Важно:** FCM-ошибка не блокирует upload. Вызов — fire-and-forget через `launch { }`.

**Новая задача:** Task 13 (FCM NotificationService).

---

### 2.4 Projects Endpoint

**Решение:** два варианта путей сосуществуют.

| Endpoint | Описание |
|----------|----------|
| `GET /api/v1/projects` | Android-клиент — проекты из workspace JWT |
| `GET /api/v1/workspaces/{id}/projects` | Явный по workspaceId (для будущего Web) |
| `POST /api/v1/workspaces/{id}/projects` | Создание проекта (Admin) |

`GET /api/v1/projects` — backend читает `workspaceId` из JWT principal, не требует параметра в URL.

**Затрагивает:** Task 11 (ProjectRoutes) — добавить `GET /projects` route.

---

### 2.5 Audit Log Writes

**Решение:** пишется из service-слоя. `AuditRepository.log()` вызывается явно. Ошибка записи не блокирует основную операцию.

**Что логируется в MVP:**

| Событие | action | resource_type |
|---------|--------|---------------|
| OTP-вход | `user.login` | `user` |
| Загрузка сборки | `build.upload` | `build` |
| Удаление сборки | `build.delete` | `build` |
| Изменение статуса сборки | `build.update` | `build` |
| Создание проекта | `project.create` | `project` |
| Удаление проекта | `project.delete` | `project` |

**Паттерн:**
```kotlin
launch { runCatching { auditRepo.log(userId, "build.upload", "build", build.id) } }
```

**Затрагивает:** Tasks 5, 9, 11 — добавить audit-writes в service-методы.

---

### 2.6 Вне MVP (Phase 2)

- Install-event / Download-event трекинг
- `GET /api/v1/audit-logs` HTTP-эндпоинт
- Notification inbox (`GET /api/v1/notifications`)
- Invite links, тестерские группы

---

## 3. Изменения в существующем плане

### Изменения в существующих задачах

| Task | Изменение |
|------|-----------|
| **Task 5** (AuthService) | Добавить workspace auto-create логику в `verifyOtp`; добавить audit write `user.login` |
| **Task 9** (BuildService) | Добавить вызов `NotificationService.notifyNewBuild()` после upload; добавить audit writes для upload/delete/update |
| **Task 11** (ProjectRoutes) | Добавить `GET /projects` (без workspaceId в пути); добавить `requireRole` вызовы; добавить audit writes для create/delete |

### Новые задачи

| Task | Название | Описание |
|------|----------|----------|
| **Task 13** | FCM NotificationService | Firebase Admin SDK init, `NotificationService`, `UserRepository.findAllByWorkspace()` |
| **Task 14** | RBAC middleware | `requireRole()` extension, `ForbiddenException`, `UnauthorizedException`; применить ко всем routes из Tasks 6, 10, 11 |
| **Task 15** | Integration tests + docker-compose | End-to-end тесты auth flow, upload flow; проверка docker-compose с PostgreSQL + MinIO |

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
