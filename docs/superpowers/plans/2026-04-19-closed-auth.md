# Closed Auth Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace open OTP-based authentication with a closed email+password system where only admins can create accounts.

**Architecture:** Backend Ktor service gets BCrypt password hashing, a new login endpoint, and admin-only user-creation/reset-password endpoints. Android replaces the two-screen OTP flow with a single login form. The Next.js admin panel replaces its OTP login with a password form and adds user management UI to the Team page.

**Tech Stack:** Ktor + Exposed (backend), Kotlin/Compose + Hilt + Retrofit (Android), Next.js 15 + React Hook Form + Zod (admin panel), BCrypt (`at.favre.lib:bcrypt:0.10.2`).

**Spec:** `docs/superpowers/specs/2026-04-19-closed-auth-design.md`

---

## File Map

### Backend (`backend/`)

| Action | File |
|--------|------|
| Modify | `build.gradle.kts` |
| Modify | `src/main/resources/application.conf` |
| Modify | `src/main/kotlin/com/appdist/config/AppConfig.kt` |
| Modify | `src/main/kotlin/com/appdist/Application.kt` |
| Modify | `src/main/kotlin/com/appdist/infrastructure/database/DatabaseFactory.kt` |
| Modify | `src/main/kotlin/com/appdist/infrastructure/database/tables/UsersTable.kt` |
| Delete | `src/main/kotlin/com/appdist/infrastructure/database/tables/OtpCodesTable.kt` |
| Delete | `src/main/kotlin/com/appdist/domain/model/OtpCode.kt` |
| Delete | `src/main/kotlin/com/appdist/domain/repository/OtpRepository.kt` |
| Delete | `src/main/kotlin/com/appdist/infrastructure/database/repository/OtpRepositoryImpl.kt` |
| Modify | `src/main/kotlin/com/appdist/domain/repository/UserRepository.kt` |
| Modify | `src/main/kotlin/com/appdist/infrastructure/database/repository/UserRepositoryImpl.kt` |
| Modify | `src/main/kotlin/com/appdist/domain/service/AuthService.kt` |
| Create | `src/main/kotlin/com/appdist/api/dto/UserDtos.kt` |
| Modify | `src/main/kotlin/com/appdist/api/routes/UserRoutes.kt` |
| Modify | `src/main/kotlin/com/appdist/api/dto/AuthDtos.kt` |
| Modify | `src/main/kotlin/com/appdist/api/routes/AuthRoutes.kt` |
| Create | `src/main/kotlin/com/appdist/api/routes/UserManagementRoutes.kt` |
| Modify | `src/main/kotlin/com/appdist/plugins/Routing.kt` |
| Modify | `src/test/kotlin/com/appdist/TestApplication.kt` |
| Modify | `src/test/kotlin/com/appdist/api/AuthRoutesTest.kt` |
| Modify | `src/test/kotlin/com/appdist/service/AuthServiceTest.kt` |
| Modify | `src/test/kotlin/com/appdist/integration/IntegrationTestBase.kt` |
| Delete | `src/test/kotlin/com/appdist/integration/AuthFlowIntegrationTest.kt` |
| Delete | `src/test/kotlin/com/appdist/integration/AuthServiceIntegrationTest.kt` |
| Modify | `src/test/kotlin/com/appdist/integration/RbacIntegrationTest.kt` |

### Android (`android/`)

| Action | File |
|--------|------|
| Modify | `core/network/src/main/kotlin/com/appdist/core/network/dto/AuthDtos.kt` |
| Modify | `core/network/src/main/kotlin/com/appdist/core/network/ApiService.kt` |
| Modify | `feature/auth/src/main/kotlin/com/appdist/feature/auth/domain/AuthRepository.kt` |
| Modify | `feature/auth/src/main/kotlin/com/appdist/feature/auth/data/AuthRepositoryImpl.kt` |
| Modify | `feature/auth/src/main/kotlin/com/appdist/feature/auth/domain/UseCases.kt` |
| Rewrite | `feature/auth/src/main/kotlin/com/appdist/feature/auth/ui/login/LoginViewModel.kt` |
| Rewrite | `feature/auth/src/main/kotlin/com/appdist/feature/auth/ui/login/LoginScreen.kt` |
| Delete | `feature/auth/src/main/kotlin/com/appdist/feature/auth/ui/otp/OtpScreen.kt` |
| Delete | `feature/auth/src/main/kotlin/com/appdist/feature/auth/ui/otp/OtpViewModel.kt` |
| Modify | `app/src/main/kotlin/com/appdist/app/navigation/AppNavGraph.kt` |
| Rewrite | `feature/auth/src/test/kotlin/com/appdist/feature/auth/LoginViewModelTest.kt` |
| Delete | `feature/auth/src/test/kotlin/com/appdist/feature/auth/OtpViewModelTest.kt` |
| Delete | `feature/auth/src/test/kotlin/com/appdist/feature/auth/RequestOtpUseCaseTest.kt` |

### Web Admin (`admin/`)

| Action | File |
|--------|------|
| Modify | `lib/types.ts` |
| Modify | `lib/api.ts` |
| Rewrite | `app/login/page.tsx` |
| Create | `components/create-user-dialog.tsx` |
| Create | `components/password-result-dialog.tsx` |
| Modify | `app/(dashboard)/team/page.tsx` |

---

## Part 1: Backend

### Task 1: Add BCrypt dependency

**Files:**
- Modify: `backend/build.gradle.kts`

- [ ] **Step 1: Add BCrypt to dependencies**

In `backend/build.gradle.kts`, add inside the `dependencies` block after the JWT line:

```kotlin
// BCrypt
implementation("at.favre.lib:bcrypt:0.10.2")
```

- [ ] **Step 2: Verify it resolves**

```bash
cd backend && ./gradlew dependencies --configuration runtimeClasspath | grep bcrypt
```

Expected output contains: `at.favre.lib:bcrypt:0.10.2`

- [ ] **Step 3: Commit**

```bash
git add backend/build.gradle.kts
git commit -m "build(backend): add BCrypt dependency"
```

---

### Task 2: Remove OTP infrastructure

**Files:**
- Delete: `backend/src/main/kotlin/com/appdist/infrastructure/database/tables/OtpCodesTable.kt`
- Delete: `backend/src/main/kotlin/com/appdist/domain/model/OtpCode.kt`
- Delete: `backend/src/main/kotlin/com/appdist/domain/repository/OtpRepository.kt`
- Delete: `backend/src/main/kotlin/com/appdist/infrastructure/database/repository/OtpRepositoryImpl.kt`
- Modify: `backend/src/main/kotlin/com/appdist/infrastructure/database/DatabaseFactory.kt`

- [ ] **Step 1: Delete OTP files**

```bash
rm backend/src/main/kotlin/com/appdist/infrastructure/database/tables/OtpCodesTable.kt
rm backend/src/main/kotlin/com/appdist/domain/model/OtpCode.kt
rm backend/src/main/kotlin/com/appdist/domain/repository/OtpRepository.kt
rm backend/src/main/kotlin/com/appdist/infrastructure/database/repository/OtpRepositoryImpl.kt
```

- [ ] **Step 2: Remove OtpCodesTable from DatabaseFactory**

In `backend/src/main/kotlin/com/appdist/infrastructure/database/DatabaseFactory.kt`, remove `OtpCodesTable` from `SchemaUtils.createMissingTablesAndColumns(...)`. The import `com.appdist.infrastructure.database.tables.*` covers other tables so just remove the reference:

```kotlin
private fun createTables() {
    transaction {
        SchemaUtils.createMissingTablesAndColumns(
            WorkspacesTable, UsersTable, ProjectsTable, BuildsTable,
            RefreshTokensTable, InstallEventsTable,
            DownloadEventsTable, AuditLogsTable
        )
    }
}
```

- [ ] **Step 3: Verify compilation (expected: errors about OTP usages in AuthService/Routing — will fix in next tasks)**

```bash
cd backend && ./gradlew compileKotlin 2>&1 | grep -E "error:|warning:" | head -20
```

- [ ] **Step 4: Commit partial state**

```bash
git add -A
git commit -m "refactor(backend): delete OTP infrastructure files"
```

---

### Task 3: Update UsersTable and UserRepository

**Files:**
- Modify: `backend/src/main/kotlin/com/appdist/infrastructure/database/tables/UsersTable.kt`
- Modify: `backend/src/main/kotlin/com/appdist/domain/repository/UserRepository.kt`
- Modify: `backend/src/main/kotlin/com/appdist/infrastructure/database/repository/UserRepositoryImpl.kt`

- [ ] **Step 1: Add `passwordHash` column to UsersTable**

Replace the full file content:

```kotlin
package com.appdist.infrastructure.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object UsersTable : Table("users") {
    val id = uuid("id")
    val workspaceId = uuid("workspace_id").references(WorkspacesTable.id).nullable()
    val email = varchar("email", 255).uniqueIndex()
    val name = varchar("name", 255)
    val role = varchar("role", 50)
    val passwordHash = varchar("password_hash", 255).default("")
    val fcmToken = varchar("fcm_token", 512).nullable()
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}
```

- [ ] **Step 2: Update UserRepository interface**

Replace the full file:

```kotlin
package com.appdist.domain.repository

import com.appdist.domain.model.User
import com.appdist.domain.model.UserRole
import java.util.UUID

interface UserRepository {
    suspend fun findById(id: UUID): User?
    suspend fun findByEmail(email: String): User?
    suspend fun findAllByWorkspace(workspaceId: UUID): List<User>
    suspend fun create(workspaceId: UUID, email: String, name: String, role: UserRole, passwordHash: String): User
    suspend fun updatePasswordHash(userId: UUID, passwordHash: String)
    suspend fun updateFcmToken(userId: UUID, token: String?)
    suspend fun update(userId: UUID, name: String): User
}
```

- [ ] **Step 3: Update UserRepositoryImpl**

Replace the full file:

```kotlin
package com.appdist.infrastructure.database.repository

import com.appdist.domain.model.User
import com.appdist.domain.model.UserRole
import com.appdist.domain.repository.UserRepository
import com.appdist.infrastructure.database.tables.UsersTable
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class UserRepositoryImpl : UserRepository {
    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    override suspend fun findById(id: UUID): User? = dbQuery {
        UsersTable.selectAll().where { UsersTable.id eq id }
            .singleOrNull()?.toUser()
    }

    override suspend fun findByEmail(email: String): User? = dbQuery {
        UsersTable.selectAll().where { UsersTable.email eq email }
            .singleOrNull()?.toUser()
    }

    override suspend fun findAllByWorkspace(workspaceId: UUID): List<User> = dbQuery {
        UsersTable.selectAll().where { UsersTable.workspaceId eq workspaceId }
            .map { it.toUser() }
    }

    override suspend fun create(
        workspaceId: UUID,
        email: String,
        name: String,
        role: UserRole,
        passwordHash: String,
    ): User {
        val id = UUID.randomUUID()
        val now = Clock.System.now()
        dbQuery {
            UsersTable.insert {
                it[UsersTable.id] = id
                it[UsersTable.workspaceId] = workspaceId
                it[UsersTable.email] = email
                it[UsersTable.name] = name
                it[UsersTable.role] = role.name
                it[UsersTable.passwordHash] = passwordHash
                it[UsersTable.fcmToken] = null
                it[UsersTable.createdAt] = now
            }
        }
        return User(id, workspaceId, email, name, role, null, now)
    }

    override suspend fun updatePasswordHash(userId: UUID, passwordHash: String) = dbQuery {
        UsersTable.update({ UsersTable.id eq userId }) {
            it[UsersTable.passwordHash] = passwordHash
        }
        Unit
    }

    override suspend fun updateFcmToken(userId: UUID, token: String?) = dbQuery {
        UsersTable.update({ UsersTable.id eq userId }) {
            it[fcmToken] = token
        }
        Unit
    }

    override suspend fun update(userId: UUID, name: String): User = dbQuery {
        UsersTable.update({ UsersTable.id eq userId }) {
            it[UsersTable.name] = name
        }
        UsersTable.selectAll().where { UsersTable.id eq userId }.single().toUser()
    }

    private fun ResultRow.toUser() = User(
        id = this[UsersTable.id],
        workspaceId = this[UsersTable.workspaceId],
        email = this[UsersTable.email],
        name = this[UsersTable.name],
        role = UserRole.valueOf(this[UsersTable.role]),
        fcmToken = this[UsersTable.fcmToken],
        createdAt = this[UsersTable.createdAt],
    )
}
```

Note: `User` domain model does not expose `passwordHash` — the hash lives only in the DB layer.

Also add `findByEmailWithHash` now so the implementation is complete before the interface update in Task 4:

```kotlin
override suspend fun findByEmailWithHash(email: String): UserWithHash? = dbQuery {
    UsersTable.selectAll().where { UsersTable.email eq email }
        .singleOrNull()?.let { row ->
            UserWithHash(row.toUser(), row[UsersTable.passwordHash])
        }
}
```

(The `UserWithHash` type will be defined in `UserRepository.kt` in Task 4 Step 4. If the compiler complains at this stage, add a temporary stub: `data class UserWithHash(val user: User, val passwordHash: String)` at the top of the file and remove it after Task 4 Step 4.)

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/kotlin/com/appdist/infrastructure/database/tables/UsersTable.kt
git add backend/src/main/kotlin/com/appdist/domain/repository/UserRepository.kt
git add backend/src/main/kotlin/com/appdist/infrastructure/database/repository/UserRepositoryImpl.kt
git commit -m "feat(backend): add password_hash to UsersTable and UserRepository"
```

---

### Task 4: Rewrite AuthService

**Files:**
- Modify: `backend/src/main/kotlin/com/appdist/domain/service/AuthService.kt`
- Modify: `backend/src/main/kotlin/com/appdist/config/AppConfig.kt`

- [ ] **Step 1: Remove OtpConfig from AppConfig**

Replace the full `AppConfig.kt`:

```kotlin
package com.appdist.config

import io.ktor.server.config.ApplicationConfig

data class AppConfig(
    val database: DatabaseConfig,
    val storage: StorageConfig,
    val jwt: JwtConfig,
    val firebase: FirebaseConfig,
    val adminBootstrap: AdminBootstrapConfig,
) {
    data class DatabaseConfig(
        val url: String,
        val user: String,
        val password: String,
        val maxPoolSize: Int,
    )
    data class StorageConfig(
        val endpoint: String,
        val accessKey: String,
        val secretKey: String,
        val bucket: String,
    )
    data class JwtConfig(
        val secret: String,
        val issuer: String,
        val audience: String,
        val accessTokenTtlMinutes: Long,
        val refreshTokenTtlDays: Long,
    )
    data class FirebaseConfig(
        val credentialsPath: String?,
    )
    data class AdminBootstrapConfig(
        val email: String?,
        val password: String?,
    )

    companion object {
        fun from(config: ApplicationConfig) = AppConfig(
            database = DatabaseConfig(
                url = config.property("database.url").getString(),
                user = config.property("database.user").getString(),
                password = config.property("database.password").getString(),
                maxPoolSize = config.property("database.maxPoolSize").getString().toInt(),
            ),
            storage = StorageConfig(
                endpoint = config.property("storage.endpoint").getString(),
                accessKey = config.property("storage.accessKey").getString(),
                secretKey = config.property("storage.secretKey").getString(),
                bucket = config.property("storage.bucket").getString(),
            ),
            jwt = JwtConfig(
                secret = config.property("jwt.secret").getString(),
                issuer = config.property("jwt.issuer").getString(),
                audience = config.property("jwt.audience").getString(),
                accessTokenTtlMinutes = config.property("jwt.accessTokenTtlMinutes").getString().toLong(),
                refreshTokenTtlDays = config.property("jwt.refreshTokenTtlDays").getString().toLong(),
            ),
            firebase = FirebaseConfig(
                credentialsPath = config.propertyOrNull("firebase.credentialsPath")?.getString(),
            ),
            adminBootstrap = AdminBootstrapConfig(
                email = config.propertyOrNull("admin.email")?.getString()
                    ?: System.getenv("ADMIN_EMAIL"),
                password = config.propertyOrNull("admin.password")?.getString()
                    ?: System.getenv("ADMIN_PASSWORD"),
            ),
        )
    }
}
```

- [ ] **Step 2: Update application.conf — remove otp block**

In `backend/src/main/resources/application.conf`, delete these lines:

```hocon
otp {
    ttlMinutes = 5
    length = 6
}
```

- [ ] **Step 3: Rewrite AuthService**

Replace the full file:

```kotlin
package com.appdist.domain.service

import at.favre.lib.crypto.bcrypt.BCrypt
import com.appdist.config.AppConfig
import com.appdist.domain.model.UserRole
import com.appdist.domain.repository.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import java.security.SecureRandom
import java.util.Date
import java.util.UUID

private val log = KotlinLogging.logger {}

private const val BCRYPT_COST = 12
private const val PASSWORD_LENGTH = 12
private val PASSWORD_CHARSET = ('A'..'Z') + ('a'..'z') + ('0'..'9')

data class AuthTokens(val accessToken: String, val refreshToken: String)

open class AuthService(
    private val userRepository: UserRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtConfig: AppConfig.JwtConfig,
    private val auditRepository: AuditRepository? = null,
) : CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.IO) {

    private val secureRandom = SecureRandom()

    suspend fun login(email: String, password: String): AuthTokens {
        require(email.isNotBlank() && password.isNotBlank()) { "Email and password required" }
        val user = userRepository.findByEmailWithHash(email)
            ?: throw InvalidCredentialsException()
        val verified = BCrypt.verifyer().verify(password.toCharArray(), user.passwordHash).verified
        if (!verified) throw InvalidCredentialsException()
        val tokens = issueTokens(user.user)
        launch {
            runCatching {
                auditRepository?.log(user.user.id, "user.login", "user", user.user.id)
            }.onFailure { log.warn(it) { "Audit log failed for user.login" } }
        }
        return tokens
    }

    suspend fun createUser(
        adminWorkspaceId: UUID,
        email: String,
        name: String,
        role: UserRole,
    ): Pair<com.appdist.domain.model.User, String> {
        require(role != UserRole.ADMIN) { "Cannot create ADMIN via this endpoint" }
        val plainPassword = generatePassword()
        val hash = BCrypt.withDefaults().hashToString(BCRYPT_COST, plainPassword.toCharArray())
        val user = userRepository.create(adminWorkspaceId, email, name, role, hash)
        return user to plainPassword
    }

    suspend fun resetPassword(userId: UUID): String {
        val plainPassword = generatePassword()
        val hash = BCrypt.withDefaults().hashToString(BCRYPT_COST, plainPassword.toCharArray())
        userRepository.updatePasswordHash(userId, hash)
        return plainPassword
    }

    suspend fun refreshToken(token: String): AuthTokens {
        val refreshToken = refreshTokenRepository.findValid(token)
            ?: error("Invalid or expired refresh token")
        refreshTokenRepository.revoke(token)
        val user = userRepository.findById(refreshToken.userId)
            ?: error("User not found")
        return issueTokens(user)
    }

    suspend fun bootstrapAdmin(email: String, password: String) {
        if (userRepository.findByEmail(email) != null) return
        val domain = email.substringAfter("@").lowercase()
        val slug = if (domain in FREE_EMAIL_DOMAINS) email.replace("@", "-at-").lowercase() else domain
        val workspace = workspaceRepository.findBySlug(slug)
            ?: workspaceRepository.create(name = slug, slug = slug)
        val hash = BCrypt.withDefaults().hashToString(BCRYPT_COST, password.toCharArray())
        val admin = userRepository.create(workspace.id, email, email.substringBefore("@"), UserRole.ADMIN, hash)
        workspaceRepository.updateOwnerId(workspace.id, admin.id)
        log.info { "Bootstrap admin created: $email" }
    }

    private fun generatePassword(): String = (1..PASSWORD_LENGTH)
        .map { PASSWORD_CHARSET[secureRandom.nextInt(PASSWORD_CHARSET.size)] }
        .joinToString("")

    private suspend fun issueTokens(user: com.appdist.domain.model.User): AuthTokens {
        requireNotNull(user.workspaceId) { "Cannot issue token for user ${user.id} with no workspace" }
        val algorithm = Algorithm.HMAC256(jwtConfig.secret)
        val accessToken = JWT.create()
            .withIssuer(jwtConfig.issuer)
            .withAudience(jwtConfig.audience)
            .withSubject(user.id.toString())
            .withClaim("email", user.email)
            .withClaim("role", user.role.name)
            .withClaim("workspace_id", user.workspaceId.toString())
            .withExpiresAt(Date.from(
                (Clock.System.now() + jwtConfig.accessTokenTtlMinutes.minutes).toJavaInstant()
            ))
            .sign(algorithm)
        val rawRefreshToken = UUID.randomUUID().toString()
        refreshTokenRepository.create(user.id, rawRefreshToken, jwtConfig.refreshTokenTtlDays)
        return AuthTokens(accessToken, rawRefreshToken)
    }
}

class InvalidCredentialsException : Exception("Invalid credentials")

val FREE_EMAIL_DOMAINS = setOf(
    "gmail.com", "googlemail.com", "yahoo.com", "yahoo.co.uk", "yahoo.co.in",
    "hotmail.com", "hotmail.co.uk", "outlook.com", "live.com", "msn.com",
    "icloud.com", "me.com", "mac.com", "protonmail.com", "proton.me",
    "aol.com", "yandex.com", "yandex.ru", "mail.ru", "inbox.com",
)
```

Note: `findByEmailWithHash` is a new method returning a wrapper with the hash — add it to `UserRepository` interface in the next step.

- [ ] **Step 4: Add `findByEmailWithHash` to UserRepository**

In `backend/src/main/kotlin/com/appdist/domain/repository/UserRepository.kt`, add:

```kotlin
data class UserWithHash(val user: User, val passwordHash: String)

interface UserRepository {
    suspend fun findById(id: UUID): User?
    suspend fun findByEmail(email: String): User?
    suspend fun findByEmailWithHash(email: String): UserWithHash?
    suspend fun findAllByWorkspace(workspaceId: UUID): List<User>
    suspend fun create(workspaceId: UUID, email: String, name: String, role: UserRole, passwordHash: String): User
    suspend fun updatePasswordHash(userId: UUID, passwordHash: String)
    suspend fun updateFcmToken(userId: UUID, token: String?)
    suspend fun update(userId: UUID, name: String): User
}
```

- [ ] **Step 5: Implement `findByEmailWithHash` in UserRepositoryImpl**

Add the implementation to `UserRepositoryImpl`:

```kotlin
override suspend fun findByEmailWithHash(email: String): UserWithHash? = dbQuery {
    UsersTable.selectAll().where { UsersTable.email eq email }
        .singleOrNull()?.let { row ->
            UserWithHash(row.toUser(), row[UsersTable.passwordHash])
        }
}
```

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/kotlin/com/appdist/config/AppConfig.kt
git add backend/src/main/resources/application.conf
git add backend/src/main/kotlin/com/appdist/domain/service/AuthService.kt
git add backend/src/main/kotlin/com/appdist/domain/repository/UserRepository.kt
git add backend/src/main/kotlin/com/appdist/infrastructure/database/repository/UserRepositoryImpl.kt
git commit -m "feat(backend): rewrite AuthService with password auth, remove OTP"
```

---

### Task 5: Update AuthRoutes and add UserManagementRoutes

**Files:**
- Modify: `backend/src/main/kotlin/com/appdist/api/dto/AuthDtos.kt`
- Modify: `backend/src/main/kotlin/com/appdist/api/routes/AuthRoutes.kt`
- Create: `backend/src/main/kotlin/com/appdist/api/routes/UserManagementRoutes.kt`
- Modify: `backend/src/main/kotlin/com/appdist/plugins/Routing.kt`
- Modify: `backend/src/main/kotlin/com/appdist/Application.kt`

- [ ] **Step 1: Extract UserDto to a shared file**

Create `backend/src/main/kotlin/com/appdist/api/dto/UserDtos.kt`:

```kotlin
package com.appdist.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val name: String,
    val role: String,
    @SerialName("workspace_id") val workspaceId: String?,
    @SerialName("created_at") val createdAt: String,
)
```

Then in `backend/src/main/kotlin/com/appdist/api/routes/UserRoutes.kt`, remove the `UserDto` data class definition (it's now imported from `dto/UserDtos.kt` via the existing `import com.appdist.api.dto.*` wildcard import). Verify the file compiles with:

```bash
cd backend && ./gradlew compileKotlin 2>&1 | grep "error:" | head -5
```

- [ ] **Step 2: Replace AuthDtos.kt**

```kotlin
package com.appdist.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RefreshTokenRequest(@SerialName("refresh_token") val refreshToken: String)

@Serializable
data class AuthResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class MessageResponse(val message: String)

@Serializable
data class CreateUserRequest(val email: String, val name: String, val role: String)

@Serializable
data class CreateUserResponse(
    val user: UserDto,
    @SerialName("generated_password") val generatedPassword: String,
)

@Serializable
data class ResetPasswordResponse(
    @SerialName("generated_password") val generatedPassword: String,
)

- [ ] **Step 2: Replace AuthRoutes.kt**

```kotlin
package com.appdist.api.routes

import com.appdist.api.dto.*
import com.appdist.domain.service.AuthService
import com.appdist.domain.service.InvalidCredentialsException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(authService: AuthService) {
    route("/auth") {
        post("/login") {
            val req = runCatching { call.receive<LoginRequest>() }.getOrElse {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_REQUEST", "email and password required"))
                return@post
            }
            if (req.email.isBlank() || req.password.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_REQUEST", "email and password required"))
                return@post
            }
            val tokens = runCatching { authService.login(req.email.trim(), req.password) }
                .getOrElse { e ->
                    when (e) {
                        is InvalidCredentialsException -> call.respond(
                            HttpStatusCode.Unauthorized,
                            ErrorResponse("INVALID_CREDENTIALS", "Invalid email or password")
                        )
                        else -> call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse("SERVER_ERROR", "Unexpected error")
                        )
                    }
                    return@post
                }
            call.respond(HttpStatusCode.OK, AuthResponse(tokens.accessToken, tokens.refreshToken))
        }

        post("/refresh") {
            val req = call.receive<RefreshTokenRequest>()
            val tokens = runCatching { authService.refreshToken(req.refreshToken) }
                .getOrElse {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("INVALID_TOKEN", "Invalid refresh token"))
                    return@post
                }
            call.respond(HttpStatusCode.OK, AuthResponse(tokens.accessToken, tokens.refreshToken))
        }
    }
}
```

- [ ] **Step 3: Create UserManagementRoutes.kt**

```kotlin
package com.appdist.api.routes

import com.appdist.api.dto.*
import com.appdist.domain.model.UserRole
import com.appdist.domain.repository.UserRepository
import com.appdist.domain.service.AuthService
import com.appdist.plugins.AuthPrincipal
import com.appdist.plugins.JWT_AUTH
import com.appdist.plugins.requireRole
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.exceptions.ExposedSQLException
import java.util.UUID

fun Route.userManagementRoutes(authService: AuthService, userRepository: UserRepository) {
    authenticate(JWT_AUTH) {
        route("/workspaces/{workspaceId}/users") {
            post {
                call.requireRole(UserRole.ADMIN)
                val workspaceId = runCatching {
                    UUID.fromString(call.parameters["workspaceId"])
                }.getOrElse {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_ID", "Invalid workspace UUID"))
                    return@post
                }
                val req = runCatching { call.receive<CreateUserRequest>() }.getOrElse {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_REQUEST", "Missing required fields"))
                    return@post
                }
                if (req.email.isBlank() || req.name.isBlank() || req.role.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_REQUEST", "email, name, role required"))
                    return@post
                }
                val role = runCatching { UserRole.valueOf(req.role.uppercase()) }.getOrElse {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_REQUEST", "Invalid role"))
                    return@post
                }
                val (user, plainPassword) = runCatching {
                    authService.createUser(workspaceId, req.email.trim(), req.name.trim(), role)
                }.getOrElse { e ->
                    if (e is ExposedSQLException && e.message?.contains("unique", ignoreCase = true) == true) {
                        call.respond(HttpStatusCode.Conflict, ErrorResponse("USER_ALREADY_EXISTS", "Email already registered"))
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, ErrorResponse("SERVER_ERROR", "Failed to create user"))
                    }
                    return@post
                }
                val principal = call.principal<AuthPrincipal>()!!
                call.respond(
                    HttpStatusCode.Created,
                    CreateUserResponse(
                        user = UserDto(
                            id = user.id.toString(),
                            email = user.email,
                            name = user.name,
                            role = user.role.name,
                            workspaceId = user.workspaceId?.toString(),
                            createdAt = user.createdAt.toString(),
                        ),
                        generatedPassword = plainPassword,
                    )
                )
            }
        }

        post("/workspaces/{workspaceId}/users/{userId}/reset-password") {
            call.requireRole(UserRole.ADMIN)
            val userId = runCatching {
                UUID.fromString(call.parameters["userId"])
            }.getOrElse {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_ID", "Invalid user UUID"))
                return@post
            }
            val user = userRepository.findById(userId)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("USER_NOT_FOUND", "User not found"))
                return@post
            }
            val newPassword = authService.resetPassword(userId)
            call.respond(HttpStatusCode.OK, ResetPasswordResponse(generatedPassword = newPassword))
        }
    }
}
```

- [ ] **Step 4: Update Routing.kt**

Replace the full file:

```kotlin
package com.appdist.plugins

import com.appdist.api.routes.*
import com.appdist.config.AppConfig
import com.appdist.domain.service.AuthService
import com.appdist.domain.service.BuildService
import com.appdist.domain.service.NotificationService
import com.appdist.infrastructure.database.repository.*
import com.appdist.infrastructure.storage.MinioStorageClient
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting(config: AppConfig) {
    val userRepo = UserRepositoryImpl()
    val workspaceRepo = WorkspaceRepositoryImpl()
    val refreshTokenRepo = RefreshTokenRepositoryImpl()
    val auditRepo = AuditRepositoryImpl()
    val buildRepo = BuildRepositoryImpl()
    val projectRepo = ProjectRepositoryImpl()

    val authService = AuthService(
        userRepository = userRepo,
        workspaceRepository = workspaceRepo,
        refreshTokenRepository = refreshTokenRepo,
        jwtConfig = config.jwt,
        auditRepository = auditRepo,
    )

    val storageClient = MinioStorageClient(config.storage)
    val notificationService = NotificationService(userRepo)

    val buildService = BuildService(
        buildRepository = buildRepo,
        storageClient = storageClient,
        auditRepository = auditRepo,
        storageConfig = config.storage,
        projectRepository = projectRepo,
        notificationService = notificationService,
    )

    routing {
        route("/api/v1") {
            authRoutes(authService)
            buildRoutes(buildService)
            uploadRoutes(buildService)
            projectRoutes(projectRepo, auditRepo, workspaceRepo)
            workspaceRoutes(workspaceRepo)
            userRoutes(userRepo)
            userManagementRoutes(authService, userRepo)
        }
    }
}
```

- [ ] **Step 5: Update Application.kt to call bootstrapAdmin after init**

In `Application.kt`, after `DatabaseFactory.init(config.database)` add:

```kotlin
// Bootstrap admin if env vars are set
config.adminBootstrap.email?.let { email ->
    config.adminBootstrap.password?.let { password ->
        kotlinx.coroutines.runBlocking {
            AuthService(
                UserRepositoryImpl(), WorkspaceRepositoryImpl(),
                RefreshTokenRepositoryImpl(), config.jwt
            ).bootstrapAdmin(email, password)
        }
    }
}
```

Add the missing imports:
```kotlin
import com.appdist.domain.service.AuthService
import com.appdist.infrastructure.database.repository.*
```

- [ ] **Step 6: Verify compilation**

```bash
cd backend && ./gradlew compileKotlin 2>&1 | grep -c "error:"
```

Expected: `0`

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/kotlin/com/appdist/
git commit -m "feat(backend): add login endpoint and admin user management routes"
```

---

### Task 6: Update TestApplication and write backend tests

**Files:**
- Modify: `backend/src/test/kotlin/com/appdist/TestApplication.kt`
- Modify: `backend/src/test/kotlin/com/appdist/api/AuthRoutesTest.kt`
- Modify: `backend/src/test/kotlin/com/appdist/service/AuthServiceTest.kt`

- [ ] **Step 1: Write failing test for login endpoint**

Replace `backend/src/test/kotlin/com/appdist/api/AuthRoutesTest.kt`:

```kotlin
package com.appdist.api

import com.appdist.testModule
import com.appdist.api.dto.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuthRoutesTest {
    @Test
    fun `login returns 200 and tokens for valid credentials`() = testApplication {
        application { testModule() }
        val client = createClient { install(ContentNegotiation) { json() } }
        // testModule seeds admin user; see TestApplication.kt
        val response = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("admin@test.com", "testpassword"))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<AuthResponse>()
        assertTrue(body.accessToken.isNotBlank())
        assertTrue(body.refreshToken.isNotBlank())
    }

    @Test
    fun `login returns 401 for wrong password`() = testApplication {
        application { testModule() }
        val client = createClient { install(ContentNegotiation) { json() } }
        val response = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("admin@test.com", "wrongpassword"))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        val body = response.body<ErrorResponse>()
        assertEquals("INVALID_CREDENTIALS", body.code)
    }

    @Test
    fun `login returns 401 for unknown email`() = testApplication {
        application { testModule() }
        val client = createClient { install(ContentNegotiation) { json() } }
        val response = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("nobody@test.com", "anything"))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `login returns 400 for blank fields`() = testApplication {
        application { testModule() }
        val client = createClient { install(ContentNegotiation) { json() } }
        val response = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("", ""))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.body<ErrorResponse>()
        assertEquals("INVALID_REQUEST", body.code)
    }
}
```

- [ ] **Step 2: Run test — expect compilation error (testModule still references OTP)**

```bash
cd backend && ./gradlew test --tests "com.appdist.api.AuthRoutesTest" 2>&1 | tail -20
```

Expected: compilation error in `TestApplication.kt`

- [ ] **Step 3: Update TestApplication.kt**

Replace the full file:

```kotlin
package com.appdist

import com.appdist.api.routes.*
import com.appdist.config.AppConfig
import com.appdist.domain.service.AuthService
import com.appdist.infrastructure.database.repository.*
import com.appdist.plugins.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking

const val TEST_ADMIN_EMAIL = "admin@test.com"
const val TEST_ADMIN_PASSWORD = "testpassword"

fun Application.testModule() {
    TestDatabase.reset()   // reset before seeding to avoid UNIQUE constraint errors on repeated calls
    TestDatabase.init()
    configureSerialization()
    configureStatusPages()

    val jwtConfig = AppConfig.JwtConfig(
        "test-secret-key-for-testing-only-32chars-min",
        "appdist", "appdist-client", 60L, 30L
    )

    val userRepo = UserRepositoryImpl()
    val workspaceRepo = WorkspaceRepositoryImpl()
    val auditRepo = AuditRepositoryImpl()
    val projectRepo = ProjectRepositoryImpl()
    val refreshTokenRepo = RefreshTokenRepositoryImpl()

    val authService = AuthService(
        userRepo, workspaceRepo, refreshTokenRepo, jwtConfig,
        auditRepository = auditRepo
    )

    // Seed admin user for tests (bootstrapAdmin is idempotent after reset)
    runBlocking { authService.bootstrapAdmin(TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD) }

    configureAuth(jwtConfig)

    routing {
        route("/api/v1") {
            authRoutes(authService)
            projectRoutes(projectRepo, auditRepo, workspaceRepo)
            workspaceRoutes(workspaceRepo)
            userRoutes(userRepo)
            userManagementRoutes(authService, userRepo)
        }
    }
}
```

- [ ] **Step 4: Run tests — expect pass**

```bash
cd backend && ./gradlew test --tests "com.appdist.api.AuthRoutesTest" 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`, 4 tests pass.

- [ ] **Step 5: Commit**

```bash
git add backend/src/test/
git commit -m "test(backend): replace OTP tests with password auth tests"
```

- [ ] **Step 6: Run all backend tests**

```bash
cd backend && ./gradlew test 2>&1 | tail -20
```

Fix any remaining failures (likely in `AuthServiceTest.kt` and integration tests that reference OTP). For each failing test: update it to use the new password-based API. Delete tests that covered OTP flows specifically.

- [ ] **Step 7: Commit test fixes**

```bash
git add backend/src/test/
git commit -m "test(backend): fix remaining tests after OTP removal"
```

---

### Task 6b: Fix integration tests

**Files:**
- Delete: `backend/src/test/kotlin/com/appdist/integration/AuthFlowIntegrationTest.kt`
- Delete: `backend/src/test/kotlin/com/appdist/integration/AuthServiceIntegrationTest.kt`
- Modify: `backend/src/test/kotlin/com/appdist/integration/IntegrationTestBase.kt`
- Modify: `backend/src/test/kotlin/com/appdist/integration/RbacIntegrationTest.kt`

- [ ] **Step 1: Delete OTP-specific integration tests**

```bash
rm backend/src/test/kotlin/com/appdist/integration/AuthFlowIntegrationTest.kt
rm backend/src/test/kotlin/com/appdist/integration/AuthServiceIntegrationTest.kt
```

These tests cover OTP flow and auto-workspace creation — behaviour that no longer exists.

- [ ] **Step 2: Rewrite IntegrationTestBase.kt**

Replace the full file:

```kotlin
package com.appdist.integration

import com.appdist.config.AppConfig
import com.appdist.domain.service.AuthService
import com.appdist.domain.service.BuildService
import com.appdist.domain.service.NotificationService
import com.appdist.infrastructure.database.DatabaseFactory
import com.appdist.infrastructure.database.repository.*
import com.appdist.infrastructure.storage.MinioStorageClient
import com.appdist.plugins.*
import com.appdist.api.routes.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking

abstract class IntegrationTestBase {
    companion object {
        const val INTEGRATION_ADMIN_EMAIL = "admin@integrationtest.com"
        const val INTEGRATION_ADMIN_PASSWORD = "integration-secret"

        val testJwtConfig = AppConfig.JwtConfig(
            secret = "integration-test-secret-key-32-chars-min",
            issuer = "appdist",
            audience = "appdist-client",
            accessTokenTtlMinutes = 60L,
            refreshTokenTtlDays = 30L,
        )
    }

    fun Application.integrationTestModule() {
        DatabaseFactory.init(IntegrationContainers.dbConfig)

        val userRepo = UserRepositoryImpl()
        val workspaceRepo = WorkspaceRepositoryImpl()
        val auditRepo = AuditRepositoryImpl()
        val projectRepo = ProjectRepositoryImpl()
        val refreshTokenRepo = RefreshTokenRepositoryImpl()

        val authService = AuthService(
            userRepo, workspaceRepo, refreshTokenRepo, testJwtConfig,
            auditRepository = auditRepo,
        )

        // Bootstrap admin for integration tests
        runBlocking { authService.bootstrapAdmin(INTEGRATION_ADMIN_EMAIL, INTEGRATION_ADMIN_PASSWORD) }

        val storageClient = MinioStorageClient(IntegrationContainers.storageConfig)
        val notificationService = NotificationService(userRepo)
        val buildRepo = BuildRepositoryImpl()
        val buildService = BuildService(
            buildRepository = buildRepo,
            storageClient = storageClient,
            auditRepository = auditRepo,
            storageConfig = IntegrationContainers.storageConfig,
            projectRepository = projectRepo,
            notificationService = notificationService,
        )

        configureSerialization()
        configureStatusPages()
        configureAuth(testJwtConfig)

        routing {
            route("/api/v1") {
                authRoutes(authService)
                buildRoutes(buildService)
                uploadRoutes(buildService)
                projectRoutes(projectRepo, auditRepo, workspaceRepo)
                workspaceRoutes(workspaceRepo)
                userRoutes(userRepo)
                userManagementRoutes(authService, userRepo)
            }
        }
    }

    /** Helper: log in as admin and return a valid access token. */
    suspend fun loginAndGetToken(
        client: io.ktor.client.HttpClient,
        email: String = INTEGRATION_ADMIN_EMAIL,
        password: String = INTEGRATION_ADMIN_PASSWORD,
    ): String {
        val response = client.post("/api/v1/auth/login") {
            io.ktor.client.request.contentType(io.ktor.http.ContentType.Application.Json)
            io.ktor.client.request.setBody(
                kotlinx.serialization.json.Json.encodeToString(
                    com.appdist.api.dto.LoginRequest(email, password)
                )
            )
        }
        val body = response.body<com.appdist.api.dto.AuthResponse>()
        return body.accessToken
    }
}
```

- [ ] **Step 3: Update RbacIntegrationTest.kt — fix loginAndGetToken calls**

Read `backend/src/test/kotlin/com/appdist/integration/RbacIntegrationTest.kt`. It calls `loginAndGetToken(email, otp)` — update all calls to `loginAndGetToken(client, email, password)` using the test admin credentials or user passwords generated via `authService.createUser(...)`. The simplest update: replace any helper that performed OTP-based login with a direct call to the new `loginAndGetToken` helper defined in `IntegrationTestBase`.

- [ ] **Step 4: Run all backend tests**

```bash
cd backend && ./gradlew test 2>&1 | tail -15
```

Expected: all pass. Fix any remaining compilation errors by removing OTP references file-by-file.

- [ ] **Step 5: Commit**

```bash
git add backend/src/test/
git commit -m "test(backend): fix integration tests after OTP removal"
```

---

## Part 2: Android

### Task 7: Replace network DTOs and ApiService

**Files:**
- Modify: `android/core/network/src/main/kotlin/com/appdist/core/network/dto/AuthDtos.kt`
- Modify: `android/core/network/src/main/kotlin/com/appdist/core/network/ApiService.kt`

- [ ] **Step 1: Update AuthDtos.kt**

Replace the full file:

```kotlin
package com.appdist.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    @SerialName("email") val email: String,
    @SerialName("password") val password: String,
)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class RefreshTokenRequest(@SerialName("refresh_token") val refreshToken: String)
```

- [ ] **Step 2: Update ApiService.kt — remove OTP methods, add login**

Replace the Auth section:

```kotlin
// Auth
@POST("api/v1/auth/login")
suspend fun login(@Body request: LoginRequest): Response<TokenResponse>

@POST("api/v1/auth/refresh")
suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<TokenResponse>

@POST("api/v1/auth/logout")
suspend fun logout(): Response<Unit>
```

Remove `requestOtp` and `verifyOtp` declarations entirely.

- [ ] **Step 3: Commit**

```bash
git add android/core/network/
git commit -m "feat(android): replace OTP network DTOs with LoginRequest"
```

---

### Task 8: Update AuthRepository and UseCases

**Files:**
- Modify: `android/feature/auth/src/main/kotlin/com/appdist/feature/auth/domain/AuthRepository.kt`
- Modify: `android/feature/auth/src/main/kotlin/com/appdist/feature/auth/data/AuthRepositoryImpl.kt`
- Modify: `android/feature/auth/src/main/kotlin/com/appdist/feature/auth/domain/UseCases.kt`

- [ ] **Step 1: Write failing test for LoginUseCase**

Create `android/feature/auth/src/test/kotlin/com/appdist/feature/auth/LoginUseCaseTest.kt`:

```kotlin
package com.appdist.feature.auth

import com.appdist.core.common.Result
import com.appdist.feature.auth.domain.AuthRepository
import com.appdist.feature.auth.domain.LoginUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs

class LoginUseCaseTest {
    private val repository: AuthRepository = mockk()
    private val useCase = LoginUseCase(repository)

    @Test
    fun `blank email returns error without calling repository`() = runTest {
        val result = useCase("", "password123")
        assertIs<Result.Error>(result)
        coVerify(exactly = 0) { repository.login(any(), any()) }
    }

    @Test
    fun `blank password returns error without calling repository`() = runTest {
        val result = useCase("user@example.com", "")
        assertIs<Result.Error>(result)
        coVerify(exactly = 0) { repository.login(any(), any()) }
    }

    @Test
    fun `valid inputs delegate to repository`() = runTest {
        coEvery { repository.login("user@example.com", "secret") } returns Result.Success(Unit)
        val result = useCase("user@example.com", "secret")
        assertIs<Result.Success<Unit>>(result)
        coVerify(exactly = 1) { repository.login("user@example.com", "secret") }
    }
}
```

- [ ] **Step 2: Delete existing OTP test files before compiling** (prevents unrelated compilation errors from masking the real failure)

```bash
rm android/feature/auth/src/test/kotlin/com/appdist/feature/auth/OtpViewModelTest.kt
rm android/feature/auth/src/test/kotlin/com/appdist/feature/auth/RequestOtpUseCaseTest.kt
```

- [ ] **Step 3: Run test — expect failure (LoginUseCase not defined yet)**

```bash
cd android && ./gradlew :feature:auth:test 2>&1 | tail -10
```

Expected: compilation error — `LoginUseCase` not found

- [ ] **Step 4: Update AuthRepository interface**

Replace the full file:

```kotlin
package com.appdist.feature.auth.domain

import com.appdist.core.common.Result
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun logout()
    val isAuthenticated: Flow<Boolean>
}
```

- [ ] **Step 5: Update AuthRepositoryImpl**

Replace the full file:

```kotlin
package com.appdist.feature.auth.data

import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import com.appdist.core.datastore.TokenManager
import com.appdist.core.network.ApiService
import com.appdist.core.network.dto.LoginRequest
import com.appdist.feature.auth.domain.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val tokenManager: TokenManager
) : AuthRepository {

    override val isAuthenticated: Flow<Boolean> = tokenManager.isAuthenticated

    override suspend fun login(email: String, password: String): Result<Unit> = try {
        val response = api.login(LoginRequest(email, password))
        if (response.isSuccessful) {
            val body = response.body()
                ?: return Result.Error(AppError.Network(response.code(), "Empty response body"))
            tokenManager.saveTokens(body.accessToken, body.refreshToken)
            Result.Success(Unit)
        } else {
            Result.Error(AppError.Network(response.code(), response.message()))
        }
    } catch (e: Exception) {
        Result.Error(e.toNetworkError())
    }

    override suspend fun logout() {
        try { api.logout() } catch (_: Exception) {}
        tokenManager.clear()
    }

    private fun Exception.toNetworkError(): AppError =
        if (this is java.io.IOException) AppError.NoInternet
        else AppError.Unknown(message ?: "Unknown error")
}
```

- [ ] **Step 6: Replace UseCases.kt with LoginUseCase**

Replace the full file:

```kotlin
package com.appdist.feature.auth.domain

import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import javax.inject.Inject

class LoginUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        if (email.isBlank()) return Result.Error(AppError.Unknown("Email cannot be blank"))
        if (password.isBlank()) return Result.Error(AppError.Unknown("Password cannot be blank"))
        return repository.login(email.trim(), password)
    }
}
```

- [ ] **Step 7: Run LoginUseCase tests — expect pass**

```bash
cd android && ./gradlew :feature:auth:test --tests "com.appdist.feature.auth.LoginUseCaseTest" 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`, 3 tests pass.

- [ ] **Step 8: Commit**

```bash
git add android/feature/auth/src/
git commit -m "feat(android): replace OTP repository/use cases with password login"
```

---

### Task 9: Rewrite LoginViewModel

**Files:**
- Rewrite: `android/feature/auth/src/main/kotlin/com/appdist/feature/auth/ui/login/LoginViewModel.kt`
- Rewrite: `android/feature/auth/src/test/kotlin/com/appdist/feature/auth/LoginViewModelTest.kt`

- [ ] **Step 1: Write failing ViewModel tests**

Replace `android/feature/auth/src/test/kotlin/com/appdist/feature/auth/LoginViewModelTest.kt`:

```kotlin
package com.appdist.feature.auth

import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import com.appdist.feature.auth.domain.AuthRepository
import com.appdist.feature.auth.domain.LoginUseCase
import com.appdist.feature.auth.ui.login.LoginAction
import com.appdist.feature.auth.ui.login.LoginEffect
import com.appdist.feature.auth.ui.login.LoginViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val repository: AuthRepository = mockk()
    private lateinit var viewModel: LoginViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = LoginViewModel(LoginUseCase(repository))
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `EmailChanged updates state`() = runTest {
        viewModel.onAction(LoginAction.EmailChanged("user@example.com"))
        assertEquals("user@example.com", viewModel.state.value.email)
    }

    @Test
    fun `PasswordChanged updates state`() = runTest {
        viewModel.onAction(LoginAction.PasswordChanged("secret"))
        assertEquals("secret", viewModel.state.value.password)
    }

    @Test
    fun `Submit with empty email shows error`() = runTest {
        viewModel.onAction(LoginAction.PasswordChanged("secret"))
        viewModel.onAction(LoginAction.Submit)
        assertNotNull(viewModel.state.value.error)
    }

    @Test
    fun `Submit success emits NavigateToHome`() = runTest {
        coEvery { repository.login("user@example.com", "secret") } returns Result.Success(Unit)
        viewModel.onAction(LoginAction.EmailChanged("user@example.com"))
        viewModel.onAction(LoginAction.PasswordChanged("secret"))

        val effectDeferred = async { viewModel.effects.first() }
        viewModel.onAction(LoginAction.Submit)
        val effect = effectDeferred.await()

        assertIs<LoginEffect.NavigateToHome>(effect)
    }

    @Test
    fun `Submit failure shows error`() = runTest {
        coEvery { repository.login(any(), any()) } returns Result.Error(AppError.NoInternet)
        viewModel.onAction(LoginAction.EmailChanged("user@example.com"))
        viewModel.onAction(LoginAction.PasswordChanged("secret"))
        viewModel.onAction(LoginAction.Submit)
        assertNotNull(viewModel.state.value.error)
        assertFalse(viewModel.state.value.isLoading)
    }
}
```

- [ ] **Step 2: Run tests — expect failure**

```bash
cd android && ./gradlew :feature:auth:test --tests "com.appdist.feature.auth.LoginViewModelTest" 2>&1 | tail -10
```

Expected: compilation error (`LoginAction.PasswordChanged` not defined yet)

- [ ] **Step 3: Rewrite LoginViewModel.kt**

```kotlin
package com.appdist.feature.auth.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import com.appdist.feature.auth.domain.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: AppError? = null,
)

sealed interface LoginAction {
    data class EmailChanged(val value: String) : LoginAction
    data class PasswordChanged(val value: String) : LoginAction
    data object Submit : LoginAction
}

sealed interface LoginEffect {
    data object NavigateToHome : LoginEffect
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _effects = Channel<LoginEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.EmailChanged -> _state.update { it.copy(email = action.value, error = null) }
            is LoginAction.PasswordChanged -> _state.update { it.copy(password = action.value, error = null) }
            LoginAction.Submit -> submit()
        }
    }

    private fun submit() {
        val email = _state.value.email.trim()
        val password = _state.value.password
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = loginUseCase(email, password)) {
                is Result.Success -> {
                    _state.update { it.copy(isLoading = false) }
                    _effects.send(LoginEffect.NavigateToHome)
                }
                is Result.Error -> _state.update { it.copy(isLoading = false, error = result.error) }
            }
        }
    }
}
```

- [ ] **Step 4: Run tests — expect pass**

```bash
cd android && ./gradlew :feature:auth:test --tests "com.appdist.feature.auth.LoginViewModelTest" 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`, 5 tests pass.

- [ ] **Step 5: Commit**

```bash
git add android/feature/auth/src/
git commit -m "feat(android): rewrite LoginViewModel for email+password auth"
```

---

### Task 10: Rewrite LoginScreen, remove OTP screens, update NavGraph

**Files:**
- Rewrite: `android/feature/auth/src/main/kotlin/com/appdist/feature/auth/ui/login/LoginScreen.kt`
- Delete: `android/feature/auth/src/main/kotlin/com/appdist/feature/auth/ui/otp/OtpScreen.kt`
- Delete: `android/feature/auth/src/main/kotlin/com/appdist/feature/auth/ui/otp/OtpViewModel.kt`
- Modify: `android/app/src/main/kotlin/com/appdist/app/navigation/AppNavGraph.kt`
- Delete: `android/feature/auth/src/test/kotlin/com/appdist/feature/auth/OtpViewModelTest.kt`
- Delete: `android/feature/auth/src/test/kotlin/com/appdist/feature/auth/RequestOtpUseCaseTest.kt`

- [ ] **Step 1: Rewrite LoginScreen.kt**

```kotlin
package com.appdist.feature.auth.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appdist.core.common.AppError

@Composable
fun LoginScreen(
    onNavigateToHome: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                LoginEffect.NavigateToHome -> onNavigateToHome()
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("AppDistribution", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = state.email,
            onValueChange = { viewModel.onAction(LoginAction.EmailChanged(it)) },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.password,
            onValueChange = { viewModel.onAction(LoginAction.PasswordChanged(it)) },
            label = { Text("Пароль") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { viewModel.onAction(LoginAction.Submit) }),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) "Скрыть пароль" else "Показать пароль",
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        state.error?.let { error ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = when (error) {
                    is AppError.NoInternet -> "Нет подключения к сети"
                    is AppError.Network -> if (error.code == 401) "Неверный логин или пароль" else "Ошибка сервера"
                    else -> "Ошибка. Попробуйте снова."
                },
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { viewModel.onAction(LoginAction.Submit) },
            enabled = state.email.isNotBlank() && state.password.isNotBlank() && !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text("Войти")
            }
        }
    }
}
```

- [ ] **Step 2: Delete OTP UI files**

```bash
rm android/feature/auth/src/main/kotlin/com/appdist/feature/auth/ui/otp/OtpScreen.kt
rm android/feature/auth/src/main/kotlin/com/appdist/feature/auth/ui/otp/OtpViewModel.kt
```

(OTP test files were already deleted in Task 8 Step 2.)

- [ ] **Step 3: Update AppNavGraph.kt**

Remove the OTP composable and update the login composable:

```kotlin
composable("auth/login") {
    LoginScreen(onNavigateToHome = {
        navController.navigate("home") { popUpTo(0) { inclusive = true } }
    })
}
```

Remove:
```kotlin
composable(
    "auth/otp/{email}",
    arguments = listOf(navArgument("email") { type = NavType.StringType })
) { backStack ->
    OtpScreen(...)
}
```

Remove imports for `OtpScreen`, `NavType`, `navArgument` (if no longer used).

- [ ] **Step 4: Build the Android app**

```bash
cd android && ./gradlew :app:assembleDebug 2>&1 | tail -15
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add android/
git commit -m "feat(android): replace OTP login flow with email+password login screen"
```

---

## Part 3: Web Admin Panel

### Task 11: Update lib/types.ts and lib/api.ts

**Files:**
- Modify: `admin/lib/types.ts`
- Modify: `admin/lib/api.ts`

- [ ] **Step 1: Add new types to lib/types.ts**

Append to the end of the file:

```ts
export interface CreateUserPayload {
  email: string;
  name: string;
  role: "UPLOADER" | "TESTER" | "VIEWER";
}

export interface CreateUserResponse {
  user: User;
  generatedPassword: string;
}

export interface ResetPasswordResponse {
  generatedPassword: string;
}
```

- [ ] **Step 2: Update lib/api.ts**

a) In the imports at the top, add the new types:
```ts
import type {
  // ... existing types ...
  CreateUserPayload,
  CreateUserResponse,
  ResetPasswordResponse,
} from "@/lib/types";
```

b) Replace the `auth` object in the `api` export:

Before:
```ts
auth: {
  requestOtp(email: string) { ... },
  verifyOtp(email: string, otp: string) { ... },
},
```

After:
```ts
auth: {
  login(email: string, password: string) {
    return request<ApiAuthTokens>("/auth/login", {
      authenticated: false,
      method: "POST",
      body: JSON.stringify({ email, password }),
    }).then(mapAuthTokens);
  },
},
```

c) In the `workspace` object, add after `getUsers`:

```ts
createUser(workspaceId: string, payload: CreateUserPayload): Promise<CreateUserResponse> {
  return request<{ user: ApiUser; generated_password: string }>(
    `/workspaces/${workspaceId}/users`,
    {
      method: "POST",
      body: JSON.stringify({
        email: payload.email,
        name: payload.name,
        role: payload.role,
      }),
    }
  ).then((res) => ({
    user: mapUser(res.user),
    generatedPassword: res.generated_password,
  }));
},
resetPassword(workspaceId: string, userId: string): Promise<ResetPasswordResponse> {
  return request<{ generated_password: string }>(
    `/workspaces/${workspaceId}/users/${userId}/reset-password`,
    { method: "POST" }
  ).then((res) => ({ generatedPassword: res.generated_password }));
},
```

- [ ] **Step 3: Commit**

```bash
git add admin/lib/types.ts admin/lib/api.ts
git commit -m "feat(admin): add password auth and user management API calls"
```

---

### Task 12: Rewrite login page

**Files:**
- Rewrite: `admin/app/login/page.tsx`

- [ ] **Step 1: Rewrite login/page.tsx**

```tsx
"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { api, ApiError } from "@/lib/api";
import { useAuth } from "@/lib/auth-context";
import { Button } from "@/components/ui/button";
import { ThemeToggle } from "@/components/theme-toggle";
import { Card, CardContent } from "@/components/ui/card";
import { Input, Label } from "@/components/ui/field";

const schema = z.object({
  email: z.string().email("Введите корректный email"),
  password: z.string().min(1, "Введите пароль"),
});

type FormValues = z.infer<typeof schema>;

export default function LoginPage() {
  const router = useRouter();
  const { saveTokens } = useAuth();
  const [error, setError] = useState<string | null>(null);

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: "", password: "" },
  });

  const onSubmit = handleSubmit(async (values) => {
    setError(null);
    try {
      const tokens = await api.auth.login(values.email, values.password);
      saveTokens(tokens);
      router.replace("/overview");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Ошибка входа. Попробуйте снова.");
    }
  });

  return (
    <div className="relative flex min-h-screen items-center justify-center px-4 py-10">
      <ThemeToggle className="absolute right-4 top-4 sm:right-6 sm:top-6" />
      <Card className="w-full max-w-lg overflow-hidden">
        <div className="bg-[var(--primary)] px-6 py-8 text-[var(--primary-contrast)]">
          <p className="text-xs uppercase tracking-[0.24em] text-[var(--primary-contrast-muted)]">AppDistribution</p>
          <h1 className="mt-3 text-3xl font-semibold">Web Admin</h1>
          <p className="mt-2 max-w-md text-sm text-[var(--primary-contrast-soft)]">
            Sign in with your credentials.
          </p>
        </div>
        <CardContent className="space-y-6 p-6">
          <form className="space-y-4" onSubmit={onSubmit}>
            <div>
              <Label>Email</Label>
              <Input placeholder="name@company.com" type="email" {...register("email")} />
              {errors.email && (
                <p className="mt-2 text-sm text-[var(--danger)]">{errors.email.message}</p>
              )}
            </div>
            <div>
              <Label>Password</Label>
              <Input placeholder="••••••••" type="password" {...register("password")} />
              {errors.password && (
                <p className="mt-2 text-sm text-[var(--danger)]">{errors.password.message}</p>
              )}
            </div>
            {error && <p className="text-sm text-[var(--danger)]">{error}</p>}
            <Button className="w-full" disabled={isSubmitting} type="submit">
              {isSubmitting ? "Signing in…" : "Sign In"}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
```

- [ ] **Step 2: Verify it builds**

```bash
cd admin && npm run build 2>&1 | tail -20
```

Expected: no errors (warnings OK)

- [ ] **Step 3: Commit**

```bash
git add admin/app/login/
git commit -m "feat(admin): replace OTP login with email+password form"
```

---

### Task 13: Add CreateUserDialog and PasswordResultDialog

**Files:**
- Create: `admin/components/create-user-dialog.tsx`
- Create: `admin/components/password-result-dialog.tsx`

- [ ] **Step 1: Create password-result-dialog.tsx**

```tsx
"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";

interface PasswordResultDialogProps {
  password: string;
  onClose: () => void;
}

export function PasswordResultDialog({ password, onClose }: PasswordResultDialogProps) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    await navigator.clipboard.writeText(password);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <Card className="w-full max-w-md mx-4">
        <CardContent className="space-y-4 p-6">
          <h2 className="text-lg font-semibold">User created</h2>
          <p className="text-sm text-[var(--text-muted)]">
            Save this password — it will not be shown again.
          </p>
          <div className="rounded-xl bg-[var(--surface-muted)] px-4 py-3 font-mono text-lg tracking-widest">
            {password}
          </div>
          <div className="flex gap-3">
            <Button variant="secondary" className="flex-1" onClick={handleCopy}>
              {copied ? "Copied!" : "Copy"}
            </Button>
            <Button className="flex-1" onClick={onClose}>
              Close
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
```

- [ ] **Step 2: Create create-user-dialog.tsx**

```tsx
"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { api, ApiError } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input, Label } from "@/components/ui/field";
import type { CreateUserPayload } from "@/lib/types";

const ROLES: CreateUserPayload["role"][] = ["UPLOADER", "TESTER", "VIEWER"];

const schema = z.object({
  email: z.string().email("Valid email required"),
  name: z.string().min(1, "Name required"),
  role: z.enum(["UPLOADER", "TESTER", "VIEWER"]),
});

type FormValues = z.infer<typeof schema>;

interface CreateUserDialogProps {
  workspaceId: string;
  onCreated: (generatedPassword: string) => void;
  onClose: () => void;
}

export function CreateUserDialog({ workspaceId, onCreated, onClose }: CreateUserDialogProps) {
  const [serverError, setServerError] = useState<string | null>(null);

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: "", name: "", role: "TESTER" },
  });

  const onSubmit = handleSubmit(async (values) => {
    setServerError(null);
    try {
      const result = await api.workspace.createUser(workspaceId, values);
      onCreated(result.generatedPassword);
    } catch (err) {
      setServerError(err instanceof ApiError ? err.message : "Failed to create user");
    }
  });

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <Card className="w-full max-w-md mx-4">
        <CardContent className="space-y-4 p-6">
          <h2 className="text-lg font-semibold">Add member</h2>
          <form className="space-y-4" onSubmit={onSubmit}>
            <div>
              <Label>Email</Label>
              <Input placeholder="name@company.com" type="email" {...register("email")} />
              {errors.email && <p className="mt-1 text-sm text-[var(--danger)]">{errors.email.message}</p>}
            </div>
            <div>
              <Label>Name</Label>
              <Input placeholder="Full name" {...register("name")} />
              {errors.name && <p className="mt-1 text-sm text-[var(--danger)]">{errors.name.message}</p>}
            </div>
            <div>
              <Label>Role</Label>
              <select
                className="mt-1 w-full rounded-lg border border-[var(--border)] bg-[var(--surface)] px-3 py-2 text-sm"
                {...register("role")}
              >
                {ROLES.map((r) => (
                  <option key={r} value={r}>{r}</option>
                ))}
              </select>
            </div>
            {serverError && <p className="text-sm text-[var(--danger)]">{serverError}</p>}
            <div className="flex gap-3">
              <Button variant="secondary" className="flex-1" type="button" onClick={onClose}>
                Cancel
              </Button>
              <Button className="flex-1" type="submit" disabled={isSubmitting}>
                {isSubmitting ? "Creating…" : "Create"}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
```

- [ ] **Step 3: Verify builds**

```bash
cd admin && npm run build 2>&1 | tail -10
```

- [ ] **Step 4: Commit**

```bash
git add admin/components/
git commit -m "feat(admin): add CreateUserDialog and PasswordResultDialog components"
```

---

### Task 14: Update Team page

**Files:**
- Modify: `admin/app/(dashboard)/team/page.tsx`

- [ ] **Step 1: Update the team page**

Replace the full file:

```tsx
"use client";

import { useState } from "react";
import { createColumnHelper } from "@tanstack/react-table";
import { KeyRound, UserPlus } from "lucide-react";
import { RoleBadge } from "@/components/domain-badges";
import { DataTable } from "@/components/data-table";
import { PageHeader } from "@/components/page-header";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { EmptyState } from "@/components/ui/empty-state";
import { CreateUserDialog } from "@/components/create-user-dialog";
import { PasswordResultDialog } from "@/components/password-result-dialog";
import { formatDateTime } from "@/lib/format";
import { useMeQuery, useTeamQuery, useWorkspaceQuery } from "@/lib/queries";
import { api } from "@/lib/api";
import type { User } from "@/lib/types";

export default function TeamPage() {
  const meQuery = useMeQuery();
  const workspaceQuery = useWorkspaceQuery();
  const isAdmin = meQuery.data?.role === "ADMIN";
  const teamQuery = useTeamQuery(workspaceQuery.data?.id, isAdmin);

  const [showCreateDialog, setShowCreateDialog] = useState(false);
  const [generatedPassword, setGeneratedPassword] = useState<string | null>(null);

  const workspaceId = workspaceQuery.data?.id;

  const handleCreated = (password: string) => {
    setShowCreateDialog(false);
    setGeneratedPassword(password);
    teamQuery.refetch();
  };

  const handleResetPassword = async (userId: string) => {
    if (!workspaceId) return;
    try {
      const result = await api.workspace.resetPassword(workspaceId, userId);
      setGeneratedPassword(result.generatedPassword);
    } catch {
      // error handled by api layer toast
    }
  };

  const columnHelper = createColumnHelper<User>();
  const columns = [
    columnHelper.accessor("name", {
      header: "Name",
      cell: (info) => (
        <div>
          <p className="font-semibold">{info.getValue()}</p>
          <p className="text-xs text-[var(--text-muted)]">{info.row.original.email}</p>
        </div>
      ),
    }),
    columnHelper.accessor("role", {
      header: "Role",
      cell: (info) => <RoleBadge role={info.getValue()} />,
    }),
    columnHelper.accessor("createdAt", {
      header: "Joined",
      cell: (info) => formatDateTime(info.getValue()),
    }),
    columnHelper.display({
      id: "fcm",
      header: "FCM",
      cell: () => <Badge tone="neutral">not exposed</Badge>,
    }),
    ...(isAdmin
      ? [
          columnHelper.display({
            id: "actions",
            header: "",
            cell: (info) => (
              <Button
                variant="secondary"
                size="sm"
                onClick={() => handleResetPassword(info.row.original.id)}
                title="Reset password"
              >
                <KeyRound className="h-4 w-4" />
              </Button>
            ),
          }),
        ]
      : []),
  ];

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="Team"
        title="Workspace members"
        description="Admins can add members and reset their passwords."
        actions={
          isAdmin ? (
            <Button onClick={() => setShowCreateDialog(true)}>
              <UserPlus className="mr-2 h-4 w-4" />
              Add member
            </Button>
          ) : undefined
        }
      />
      {!isAdmin ? (
        <EmptyState
          title="Admins only"
          description="The workspace user list is visible to admins only."
        />
      ) : (
        <Card>
          <CardHeader title="People with access" />
          <CardContent>
            {teamQuery.data?.length ? (
              <DataTable columns={columns} data={teamQuery.data} />
            ) : (
              <EmptyState title="No members" description="Add members using the button above." />
            )}
          </CardContent>
        </Card>
      )}

      {showCreateDialog && workspaceId && (
        <CreateUserDialog
          workspaceId={workspaceId}
          onCreated={handleCreated}
          onClose={() => setShowCreateDialog(false)}
        />
      )}

      {generatedPassword && (
        <PasswordResultDialog
          password={generatedPassword}
          onClose={() => setGeneratedPassword(null)}
        />
      )}
    </div>
  );
}
```

Note: `PageHeader` may not currently have an `action` prop — check `components/page-header.tsx` and add it if missing.

- [ ] **Step 2: Verify PageHeader signature**

`PageHeader` already has an `actions` prop (plural, `ReactNode`) — the Team page uses `actions={...}` (plural), which is correct as written above. No change needed to `page-header.tsx`.

- [ ] **Step 3: Build**

```bash
cd admin && npm run build 2>&1 | tail -15
```

Expected: `BUILD SUCCESSFUL` (no type errors)

- [ ] **Step 4: Commit**

```bash
git add admin/app/ admin/components/
git commit -m "feat(admin): add user creation and password reset to Team page"
```

---

## Final Verification

### Task 15: End-to-end smoke test

- [ ] **Step 1: Run all backend tests**

```bash
cd backend && ./gradlew test 2>&1 | tail -10
```

Expected: all pass.

- [ ] **Step 2: Run all Android unit tests**

```bash
cd android && ./gradlew test 2>&1 | tail -10
```

Expected: all pass.

- [ ] **Step 3: Build Android APK**

```bash
cd android && ./gradlew :app:assembleDebug 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Build admin panel**

```bash
cd admin && npm run build 2>&1 | tail -5
```

Expected: no errors.

- [ ] **Step 5: Final commit**

```bash
git add -A
git commit -m "feat: closed auth — replace OTP with email+password across backend, Android, admin"
```
