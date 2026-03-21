# AppDistribution Backend MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Собрать production-ready Ktor backend с JWT auth, управлением сборками, upload/download APK через MinIO, push-уведомлениями и audit log.

**Architecture:** Ktor 3.x + Exposed ORM + PostgreSQL + MinIO. Clean layered architecture: api → domain → infrastructure. Domain entities изолированы от DTOs и DB-схемы.

**Tech Stack:** Kotlin 2.x, Ktor 3.x, Exposed 0.55+, PostgreSQL 16, MinIO, kotlinx.serialization, HikariCP, BCrypt, JWT (java-jwt), Apache commons-compress (APK parsing)

---

## File Structure

```
backend/
├── build.gradle.kts
├── gradle.properties
├── settings.gradle.kts
├── Dockerfile
├── docker-compose.yml
├── src/
│   ├── main/
│   │   ├── kotlin/com/appdist/
│   │   │   ├── Application.kt
│   │   │   ├── config/
│   │   │   │   ├── AppConfig.kt
│   │   │   │   └── Environment.kt
│   │   │   ├── plugins/
│   │   │   │   ├── Authentication.kt
│   │   │   │   ├── Routing.kt
│   │   │   │   ├── Serialization.kt
│   │   │   │   ├── StatusPages.kt
│   │   │   │   └── CORS.kt
│   │   │   ├── domain/
│   │   │   │   ├── model/
│   │   │   │   │   ├── User.kt
│   │   │   │   │   ├── Workspace.kt
│   │   │   │   │   ├── Project.kt
│   │   │   │   │   ├── Build.kt
│   │   │   │   │   ├── OtpCode.kt
│   │   │   │   │   ├── RefreshToken.kt
│   │   │   │   │   ├── InstallEvent.kt
│   │   │   │   │   ├── DownloadEvent.kt
│   │   │   │   │   └── AuditLog.kt
│   │   │   │   ├── repository/
│   │   │   │   │   ├── UserRepository.kt
│   │   │   │   │   ├── WorkspaceRepository.kt
│   │   │   │   │   ├── ProjectRepository.kt
│   │   │   │   │   ├── BuildRepository.kt
│   │   │   │   │   └── AuditRepository.kt
│   │   │   │   └── service/
│   │   │   │       ├── AuthService.kt
│   │   │   │       ├── BuildService.kt
│   │   │   │       ├── StorageService.kt
│   │   │   │       └── NotificationService.kt
│   │   │   ├── api/
│   │   │   │   ├── routes/
│   │   │   │   │   ├── AuthRoutes.kt
│   │   │   │   │   ├── WorkspaceRoutes.kt
│   │   │   │   │   ├── ProjectRoutes.kt
│   │   │   │   │   ├── BuildRoutes.kt
│   │   │   │   │   ├── UploadRoutes.kt
│   │   │   │   │   ├── EventRoutes.kt
│   │   │   │   │   └── UserRoutes.kt
│   │   │   │   └── dto/
│   │   │   │       ├── AuthDtos.kt
│   │   │   │       ├── WorkspaceDtos.kt
│   │   │   │       ├── ProjectDtos.kt
│   │   │   │       ├── BuildDtos.kt
│   │   │   │       └── ErrorResponse.kt
│   │   │   └── infrastructure/
│   │   │       ├── database/
│   │   │       │   ├── DatabaseFactory.kt
│   │   │       │   ├── tables/
│   │   │       │   │   ├── UsersTable.kt
│   │   │       │   │   ├── WorkspacesTable.kt
│   │   │       │   │   ├── ProjectsTable.kt
│   │   │       │   │   ├── BuildsTable.kt
│   │   │       │   │   ├── OtpCodesTable.kt
│   │   │       │   │   ├── RefreshTokensTable.kt
│   │   │       │   │   ├── InstallEventsTable.kt
│   │   │       │   │   ├── DownloadEventsTable.kt
│   │   │       │   │   └── AuditLogsTable.kt
│   │   │       │   └── repository/
│   │   │       │       ├── UserRepositoryImpl.kt
│   │   │       │       ├── WorkspaceRepositoryImpl.kt
│   │   │       │       ├── ProjectRepositoryImpl.kt
│   │   │       │       ├── BuildRepositoryImpl.kt
│   │   │       │       └── AuditRepositoryImpl.kt
│   │   │       ├── storage/
│   │   │       │   ├── StorageClient.kt
│   │   │       │   └── MinioStorageClient.kt
│   │   │       └── apk/
│   │   │           └── ApkMetadataExtractor.kt
│   │   └── resources/
│   │       └── application.conf
│   └── test/kotlin/com/appdist/
│       ├── TestApplication.kt
│       ├── api/
│       │   ├── AuthRoutesTest.kt
│       │   ├── BuildRoutesTest.kt
│       │   └── UploadRoutesTest.kt
│       └── service/
│           ├── AuthServiceTest.kt
│           └── BuildServiceTest.kt
```

---

## Task 1: Project Setup — Gradle + Docker

**Files:**
- Create: `backend/build.gradle.kts`
- Create: `backend/settings.gradle.kts`
- Create: `backend/gradle.properties`
- Create: `backend/docker-compose.yml`
- Create: `backend/Dockerfile`
- Create: `backend/src/main/resources/application.conf`

- [ ] **Step 1: Создать `backend/settings.gradle.kts`**

```kotlin
rootProject.name = "appdist-backend"
```

- [ ] **Step 2: Создать `backend/gradle.properties`**

```properties
kotlin.code.style=official
ktor_version=3.1.1
kotlin_version=2.1.10
logback_version=1.5.12
exposed_version=0.58.0
hikari_version=6.2.1
minio_version=8.5.11
java_jwt_version=4.4.0
```

- [ ] **Step 3: Создать `backend/build.gradle.kts`**

```kotlin
plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
    id("io.ktor.plugin") version "3.1.1"
}

group = "com.appdist"
version = "0.1.0"

application {
    mainClass.set("com.appdist.ApplicationKt")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=true")
}

repositories {
    mavenCentral()
}

dependencies {
    // Ktor server
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")
    implementation("io.ktor:ktor-server-auth-jvm")
    implementation("io.ktor:ktor-server-auth-jwt-jvm")
    implementation("io.ktor:ktor-server-cors-jvm")
    implementation("io.ktor:ktor-server-status-pages-jvm")
    implementation("io.ktor:ktor-server-call-logging-jvm")
    implementation("io.ktor:ktor-server-request-validation-jvm")

    // Database
    implementation("org.jetbrains.exposed:exposed-core:${property("exposed_version")}")
    implementation("org.jetbrains.exposed:exposed-dao:${property("exposed_version")}")
    implementation("org.jetbrains.exposed:exposed-jdbc:${property("exposed_version")}")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:${property("exposed_version")}")
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("com.zaxxer:HikariCP:${property("hikari_version")}")

    // MinIO
    implementation("io.minio:minio:${property("minio_version")}")

    // JWT
    implementation("com.auth0:java-jwt:${property("java_jwt_version")}")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

    // APK parsing
    implementation("net.dongliu:apk-parser:2.6.10")

    // Logging
    implementation("ch.qos.logback:logback-classic:${property("logback_version")}")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")

    // Test
    testImplementation("io.ktor:ktor-server-test-host-jvm")
    testImplementation("io.ktor:ktor-client-content-negotiation-jvm")
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("com.h2database:h2:2.3.232")
}

tasks.test {
    useJUnitPlatform()
}
```

- [ ] **Step 4: Создать `backend/docker-compose.yml`**

```yaml
version: '3.9'

services:
  postgres:
    image: postgres:16-alpine
    container_name: appdist-postgres
    environment:
      POSTGRES_USER: appdist
      POSTGRES_PASSWORD: appdist_secret
      POSTGRES_DB: appdist
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  minio:
    image: minio/minio:latest
    container_name: appdist-minio
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin123
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - minio_data:/data

volumes:
  postgres_data:
  minio_data:
```

- [ ] **Step 5: Создать `backend/src/main/resources/application.conf`**

```hocon
ktor {
    deployment {
        port = 8080
        port = ${?PORT}
    }
    application {
        modules = [ com.appdist.ApplicationKt.module ]
    }
}

database {
    url = "jdbc:postgresql://localhost:5432/appdist"
    url = ${?DATABASE_URL}
    user = "appdist"
    user = ${?DATABASE_USER}
    password = "appdist_secret"
    password = ${?DATABASE_PASSWORD}
    maxPoolSize = 10
}

storage {
    endpoint = "http://localhost:9000"
    endpoint = ${?MINIO_ENDPOINT}
    accessKey = "minioadmin"
    accessKey = ${?MINIO_ACCESS_KEY}
    secretKey = "minioadmin123"
    secretKey = ${?MINIO_SECRET_KEY}
    bucket = "appdist-builds"
    bucket = ${?MINIO_BUCKET}
}

jwt {
    secret = "change-me-in-production-use-256-bit-key"
    secret = ${?JWT_SECRET}
    issuer = "appdist"
    audience = "appdist-client"
    accessTokenTtlMinutes = 60
    refreshTokenTtlDays = 30
}

otp {
    ttlMinutes = 5
    length = 6
}
```

- [ ] **Step 6: Запустить инфраструктуру**

```bash
cd backend
docker-compose up -d
```

Expected: postgres и minio запущены, порты 5432 и 9000 открыты.

- [ ] **Step 7: Commit**

```bash
git add backend/
git commit -m "chore: init backend project structure and docker-compose"
```

---

## Task 2: Domain Models

**Files:**
- Create: `backend/src/main/kotlin/com/appdist/domain/model/User.kt`
- Create: `backend/src/main/kotlin/com/appdist/domain/model/Workspace.kt`
- Create: `backend/src/main/kotlin/com/appdist/domain/model/Project.kt`
- Create: `backend/src/main/kotlin/com/appdist/domain/model/Build.kt`
- Create: `backend/src/main/kotlin/com/appdist/domain/model/OtpCode.kt`
- Create: `backend/src/main/kotlin/com/appdist/domain/model/RefreshToken.kt`
- Create: `backend/src/main/kotlin/com/appdist/domain/model/InstallEvent.kt`
- Create: `backend/src/main/kotlin/com/appdist/domain/model/AuditLog.kt`

- [ ] **Step 1: Написать domain models**

`backend/src/main/kotlin/com/appdist/domain/model/User.kt`:
```kotlin
package com.appdist.domain.model

import kotlinx.datetime.Instant
import java.util.UUID

enum class UserRole { ADMIN, UPLOADER, TESTER, VIEWER }

data class User(
    val id: UUID,
    val workspaceId: UUID,
    val email: String,
    val name: String,
    val role: UserRole,
    val fcmToken: String?,
    val createdAt: Instant,
)
```

`backend/src/main/kotlin/com/appdist/domain/model/Workspace.kt`:
```kotlin
package com.appdist.domain.model

import kotlinx.datetime.Instant
import java.util.UUID

data class Workspace(
    val id: UUID,
    val name: String,
    val slug: String,
    val ownerId: UUID,
    val createdAt: Instant,
)
```

`backend/src/main/kotlin/com/appdist/domain/model/Project.kt`:
```kotlin
package com.appdist.domain.model

import kotlinx.datetime.Instant
import java.util.UUID

data class Project(
    val id: UUID,
    val workspaceId: UUID,
    val name: String,
    val packageName: String,
    val iconUrl: String?,
    val createdAt: Instant,
)
```

`backend/src/main/kotlin/com/appdist/domain/model/Build.kt`:
```kotlin
package com.appdist.domain.model

import kotlinx.datetime.Instant
import java.util.UUID

enum class BuildEnvironment { DEV, QA, STAGING, PROD_LIKE }
enum class ReleaseChannel { NIGHTLY, ALPHA, BETA, RC, INTERNAL, CUSTOM }
enum class BuildStatus { ACTIVE, DEPRECATED, ARCHIVED, MANDATORY }

data class Build(
    val id: UUID,
    val projectId: UUID,
    val versionName: String,
    val versionCode: Long,
    val buildNumber: String?,
    val flavor: String?,
    val buildType: String,
    val environment: BuildEnvironment,
    val channel: ReleaseChannel,
    val branch: String?,
    val commitHash: String?,
    val uploaderId: UUID,
    val uploadDate: Instant,
    val changelog: String?,
    val fileSizeBytes: Long,
    val checksumSha256: String,
    val minSdk: Int,
    val targetSdk: Int,
    val certFingerprint: String?,
    val abis: List<String>,
    val storageKey: String,
    val status: BuildStatus,
    val expiryDate: Instant?,
    val isLatestInChannel: Boolean,
)
```

`backend/src/main/kotlin/com/appdist/domain/model/OtpCode.kt`:
```kotlin
package com.appdist.domain.model

import kotlinx.datetime.Instant

data class OtpCode(
    val email: String,
    val code: String,
    val expiresAt: Instant,
    val used: Boolean,
)
```

`backend/src/main/kotlin/com/appdist/domain/model/RefreshToken.kt`:
```kotlin
package com.appdist.domain.model

import kotlinx.datetime.Instant
import java.util.UUID

data class RefreshToken(
    val id: UUID,
    val userId: UUID,
    val token: String,
    val expiresAt: Instant,
    val revoked: Boolean,
)
```

`backend/src/main/kotlin/com/appdist/domain/model/InstallEvent.kt`:
```kotlin
package com.appdist.domain.model

import kotlinx.datetime.Instant
import java.util.UUID

data class DeviceInfo(
    val model: String?,
    val androidVersion: String?,
    val manufacturer: String?,
)

data class InstallEvent(
    val id: UUID,
    val userId: UUID,
    val buildId: UUID,
    val installedAt: Instant,
    val deviceInfo: DeviceInfo,
)
```

`backend/src/main/kotlin/com/appdist/domain/model/AuditLog.kt`:
```kotlin
package com.appdist.domain.model

import kotlinx.datetime.Instant
import java.util.UUID

data class AuditLog(
    val id: UUID,
    val userId: UUID,
    val action: String,
    val resourceType: String,
    val resourceId: UUID?,
    val metadata: Map<String, String>?,
    val createdAt: Instant,
)
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/kotlin/com/appdist/domain/model/
git commit -m "feat: add domain models (User, Project, Build, events)"
```

---

## Task 3: Database Layer — Exposed Tables + DatabaseFactory

**Files:**
- Create: `backend/src/main/kotlin/com/appdist/infrastructure/database/DatabaseFactory.kt`
- Create: `backend/src/main/kotlin/com/appdist/infrastructure/database/tables/UsersTable.kt`
- Create: `backend/src/main/kotlin/com/appdist/infrastructure/database/tables/WorkspacesTable.kt`
- Create: `backend/src/main/kotlin/com/appdist/infrastructure/database/tables/ProjectsTable.kt`
- Create: `backend/src/main/kotlin/com/appdist/infrastructure/database/tables/BuildsTable.kt`
- Create: `backend/src/main/kotlin/com/appdist/infrastructure/database/tables/OtpCodesTable.kt`
- Create: `backend/src/main/kotlin/com/appdist/infrastructure/database/tables/RefreshTokensTable.kt`
- Create: `backend/src/main/kotlin/com/appdist/infrastructure/database/tables/InstallEventsTable.kt`
- Create: `backend/src/main/kotlin/com/appdist/infrastructure/database/tables/AuditLogsTable.kt`

- [ ] **Step 1: Написать failing test для DatabaseFactory**

`backend/src/test/kotlin/com/appdist/TestDatabase.kt`:
```kotlin
package com.appdist

import com.appdist.infrastructure.database.DatabaseFactory
import com.appdist.infrastructure.database.tables.*
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object TestDatabase {
    fun init() {
        DatabaseFactory.initH2()
        transaction {
            SchemaUtils.create(
                WorkspacesTable, UsersTable, ProjectsTable, BuildsTable,
                OtpCodesTable, RefreshTokensTable, InstallEventsTable, AuditLogsTable
            )
        }
    }
}
```

`backend/src/test/kotlin/com/appdist/infrastructure/DatabaseTest.kt`:
```kotlin
package com.appdist.infrastructure

import com.appdist.TestDatabase
import com.appdist.infrastructure.database.tables.UsersTable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.Test
import kotlin.test.assertEquals

class DatabaseTest {
    @Test
    fun `tables created successfully`() {
        TestDatabase.init()
        transaction {
            val count = UsersTable.selectAll().count()
            assertEquals(0L, count)
        }
    }
}
```

- [ ] **Step 2: Run test — убедиться что падает (нет реализации)**

```bash
cd backend && ./gradlew test --tests "com.appdist.infrastructure.DatabaseTest" 2>&1 | tail -20
```

Expected: FAIL — compilation error, `DatabaseFactory` not found.

- [ ] **Step 3: Реализовать DatabaseFactory и Tables**

`backend/src/main/kotlin/com/appdist/infrastructure/database/DatabaseFactory.kt`:
```kotlin
package com.appdist.infrastructure.database

import com.appdist.config.AppConfig
import com.appdist.infrastructure.database.tables.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init(config: AppConfig.DatabaseConfig) {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.url
            username = config.user
            password = config.password
            maximumPoolSize = config.maxPoolSize
            driverClassName = "org.postgresql.Driver"
        }
        Database.connect(HikariDataSource(hikariConfig))
        createTables()
    }

    fun initH2() {
        Database.connect(
            url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
            driver = "org.h2.Driver"
        )
    }

    private fun createTables() {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                WorkspacesTable, UsersTable, ProjectsTable, BuildsTable,
                OtpCodesTable, RefreshTokensTable, InstallEventsTable,
                DownloadEventsTable, AuditLogsTable
            )
        }
    }
}
```

`backend/src/main/kotlin/com/appdist/infrastructure/database/tables/WorkspacesTable.kt`:
```kotlin
package com.appdist.infrastructure.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object WorkspacesTable : Table("workspaces") {
    val id = uuid("id")
    val name = varchar("name", 255)
    val slug = varchar("slug", 100).uniqueIndex()
    val ownerId = uuid("owner_id")
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}
```

`backend/src/main/kotlin/com/appdist/infrastructure/database/tables/UsersTable.kt`:
```kotlin
package com.appdist.infrastructure.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object UsersTable : Table("users") {
    val id = uuid("id")
    val workspaceId = uuid("workspace_id").references(WorkspacesTable.id)
    val email = varchar("email", 255).uniqueIndex()
    val name = varchar("name", 255)
    val role = varchar("role", 50)
    val fcmToken = varchar("fcm_token", 512).nullable()
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}
```

`backend/src/main/kotlin/com/appdist/infrastructure/database/tables/ProjectsTable.kt`:
```kotlin
package com.appdist.infrastructure.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object ProjectsTable : Table("projects") {
    val id = uuid("id")
    val workspaceId = uuid("workspace_id").references(WorkspacesTable.id)
    val name = varchar("name", 255)
    val packageName = varchar("package_name", 255)
    val iconUrl = varchar("icon_url", 1024).nullable()
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}
```

`backend/src/main/kotlin/com/appdist/infrastructure/database/tables/BuildsTable.kt`:
```kotlin
package com.appdist.infrastructure.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object BuildsTable : Table("builds") {
    val id = uuid("id")
    val projectId = uuid("project_id").references(ProjectsTable.id)
    val versionName = varchar("version_name", 100)
    val versionCode = long("version_code")
    val buildNumber = varchar("build_number", 100).nullable()
    val flavor = varchar("flavor", 100).nullable()
    val buildType = varchar("build_type", 50)
    val environment = varchar("environment", 50)
    val channel = varchar("channel", 50)
    val branch = varchar("branch", 255).nullable()
    val commitHash = varchar("commit_hash", 64).nullable()
    val uploaderId = uuid("uploader_id").references(UsersTable.id)
    val uploadDate = timestamp("upload_date")
    val changelog = text("changelog").nullable()
    val fileSizeBytes = long("file_size_bytes")
    val checksumSha256 = varchar("checksum_sha256", 64)
    val minSdk = integer("min_sdk")
    val targetSdk = integer("target_sdk")
    val certFingerprint = varchar("cert_fingerprint", 256).nullable()
    val abis = varchar("abis", 512)  // JSON array stored as string
    val storageKey = varchar("storage_key", 1024)
    val status = varchar("status", 50)
    val expiryDate = timestamp("expiry_date").nullable()
    val isLatestInChannel = bool("is_latest_in_channel").default(false)
    override val primaryKey = PrimaryKey(id)
}
```

`backend/src/main/kotlin/com/appdist/infrastructure/database/tables/OtpCodesTable.kt`:
```kotlin
package com.appdist.infrastructure.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object OtpCodesTable : Table("otp_codes") {
    val email = varchar("email", 255)
    val code = varchar("code", 10)
    val expiresAt = timestamp("expires_at")
    val used = bool("used").default(false)
    override val primaryKey = PrimaryKey(email, code)
}
```

`backend/src/main/kotlin/com/appdist/infrastructure/database/tables/RefreshTokensTable.kt`:
```kotlin
package com.appdist.infrastructure.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object RefreshTokensTable : Table("refresh_tokens") {
    val id = uuid("id")
    val userId = uuid("user_id").references(UsersTable.id)
    val token = varchar("token", 512).uniqueIndex()
    val expiresAt = timestamp("expires_at")
    val revoked = bool("revoked").default(false)
    override val primaryKey = PrimaryKey(id)
}
```

`backend/src/main/kotlin/com/appdist/infrastructure/database/tables/InstallEventsTable.kt`:
```kotlin
package com.appdist.infrastructure.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object InstallEventsTable : Table("install_events") {
    val id = uuid("id")
    val userId = uuid("user_id").references(UsersTable.id)
    val buildId = uuid("build_id").references(BuildsTable.id)
    val installedAt = timestamp("installed_at")
    val deviceModel = varchar("device_model", 255).nullable()
    val androidVersion = varchar("android_version", 50).nullable()
    val manufacturer = varchar("manufacturer", 255).nullable()
    override val primaryKey = PrimaryKey(id)
}
```

`backend/src/main/kotlin/com/appdist/infrastructure/database/tables/DownloadEventsTable.kt`:
```kotlin
package com.appdist.infrastructure.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object DownloadEventsTable : Table("download_events") {
    val id = uuid("id")
    val userId = uuid("user_id").references(UsersTable.id)
    val buildId = uuid("build_id").references(BuildsTable.id)
    val downloadedAt = timestamp("downloaded_at")
    override val primaryKey = PrimaryKey(id)
}
```

`backend/src/main/kotlin/com/appdist/infrastructure/database/tables/AuditLogsTable.kt`:
```kotlin
package com.appdist.infrastructure.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object AuditLogsTable : Table("audit_logs") {
    val id = uuid("id")
    val userId = uuid("user_id").references(UsersTable.id)
    val action = varchar("action", 100)
    val resourceType = varchar("resource_type", 100)
    val resourceId = uuid("resource_id").nullable()
    val metadata = text("metadata").nullable()  // JSON
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}
```

- [ ] **Step 4: Run test — убедиться что проходит**

```bash
cd backend && ./gradlew test --tests "com.appdist.infrastructure.DatabaseTest"
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add backend/src/
git commit -m "feat: add database tables and DatabaseFactory with H2 test support"
```

---

## Task 4: Config + Application Entry Point

**Files:**
- Create: `backend/src/main/kotlin/com/appdist/config/AppConfig.kt`
- Create: `backend/src/main/kotlin/com/appdist/Application.kt`
- Create: `backend/src/main/kotlin/com/appdist/plugins/Serialization.kt`
- Create: `backend/src/main/kotlin/com/appdist/plugins/StatusPages.kt`
- Create: `backend/src/main/kotlin/com/appdist/plugins/CORS.kt`
- Create: `backend/src/main/kotlin/com/appdist/api/dto/ErrorResponse.kt`

- [ ] **Step 1: AppConfig.kt**

```kotlin
package com.appdist.config

import io.ktor.server.config.ApplicationConfig

data class AppConfig(
    val database: DatabaseConfig,
    val storage: StorageConfig,
    val jwt: JwtConfig,
    val otp: OtpConfig,
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
    data class OtpConfig(
        val ttlMinutes: Long,
        val length: Int,
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
            otp = OtpConfig(
                ttlMinutes = config.property("otp.ttlMinutes").getString().toLong(),
                length = config.property("otp.length").getString().toInt(),
            )
        )
    }
}
```

- [ ] **Step 2: ErrorResponse.kt**

```kotlin
package com.appdist.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val code: String,
    val message: String,
    val details: Map<String, String>? = null,
)
```

- [ ] **Step 3: Serialization plugin**

`backend/src/main/kotlin/com/appdist/plugins/Serialization.kt`:
```kotlin
package com.appdist.plugins

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = false
            isLenient = false
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }
}
```

- [ ] **Step 4: StatusPages plugin**

`backend/src/main/kotlin/com/appdist/plugins/StatusPages.kt`:
```kotlin
package com.appdist.plugins

import com.appdist.api.dto.ErrorResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

class NotFoundException(message: String) : Exception(message)
class ForbiddenException(message: String = "Access denied") : Exception(message)
class ConflictException(message: String) : Exception(message)
class BadRequestException(message: String) : Exception(message)

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<NotFoundException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", cause.message ?: "Not found"))
        }
        exception<ForbiddenException> { call, cause ->
            call.respond(HttpStatusCode.Forbidden, ErrorResponse("FORBIDDEN", cause.message ?: "Forbidden"))
        }
        exception<ConflictException> { call, cause ->
            call.respond(HttpStatusCode.Conflict, ErrorResponse("CONFLICT", cause.message ?: "Conflict"))
        }
        exception<BadRequestException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", cause.message ?: "Bad request"))
        }
        exception<Throwable> { call, cause ->
            application.log.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("INTERNAL_ERROR", "Internal server error")
            )
        }
    }
}
```

- [ ] **Step 5: CORS plugin**

`backend/src/main/kotlin/com/appdist/plugins/CORS.kt`:
```kotlin
package com.appdist.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

fun Application.configureCORS() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        anyHost() // TODO: restrict in production
    }
}
```

- [ ] **Step 6: Application.kt entry point**

```kotlin
package com.appdist

import com.appdist.config.AppConfig
import com.appdist.infrastructure.database.DatabaseFactory
import com.appdist.plugins.*
import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    val config = AppConfig.from(environment.config)

    DatabaseFactory.init(config.database)

    configureSerialization()
    configureCORS()
    configureStatusPages()
    configureAuth(config.jwt)
    configureRouting(config)
}
```

- [ ] **Step 7: Убедиться что компилируется**

```bash
cd backend && ./gradlew compileKotlin 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/kotlin/com/appdist/
git commit -m "feat: add application entry point, config, and Ktor plugins"
```

---

## Task 5: Repository Interfaces + Auth Service

**Files:**
- Create: `backend/src/main/kotlin/com/appdist/domain/repository/UserRepository.kt`
- Create: `backend/src/main/kotlin/com/appdist/domain/repository/WorkspaceRepository.kt`
- Create: `backend/src/main/kotlin/com/appdist/domain/repository/BuildRepository.kt`
- Create: `backend/src/main/kotlin/com/appdist/domain/repository/AuditRepository.kt`
- Create: `backend/src/main/kotlin/com/appdist/domain/service/AuthService.kt`
- Create: `backend/src/main/kotlin/com/appdist/infrastructure/database/repository/UserRepositoryImpl.kt`
- Create: `backend/src/test/kotlin/com/appdist/service/AuthServiceTest.kt`

- [ ] **Step 1: Написать failing test для AuthService**

`backend/src/test/kotlin/com/appdist/service/AuthServiceTest.kt`:
```kotlin
package com.appdist.service

import com.appdist.TestDatabase
import com.appdist.config.AppConfig
import com.appdist.domain.model.UserRole
import com.appdist.domain.service.AuthService
import com.appdist.infrastructure.database.repository.UserRepositoryImpl
import com.appdist.infrastructure.database.repository.WorkspaceRepositoryImpl
import com.appdist.infrastructure.database.repository.OtpRepositoryImpl
import com.appdist.infrastructure.database.repository.RefreshTokenRepositoryImpl
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertEquals

class AuthServiceTest {
    private lateinit var authService: AuthService
    private val jwtConfig = AppConfig.JwtConfig(
        secret = "test-secret-key-256-bit-minimum-length",
        issuer = "appdist",
        audience = "appdist-client",
        accessTokenTtlMinutes = 60L,
        refreshTokenTtlDays = 30L,
    )
    private val otpConfig = AppConfig.OtpConfig(ttlMinutes = 5L, length = 6)

    @BeforeTest
    fun setup() {
        TestDatabase.init()
        authService = AuthService(
            userRepository = UserRepositoryImpl(),
            workspaceRepository = WorkspaceRepositoryImpl(),
            otpRepository = OtpRepositoryImpl(),
            refreshTokenRepository = RefreshTokenRepositoryImpl(),
            jwtConfig = jwtConfig,
            otpConfig = otpConfig,
        )
    }

    @Test
    fun `requestOtp creates OTP for new user flow`() = runTest {
        val email = "test@example.com"
        val otp = authService.requestOtp(email)
        assertNotNull(otp)
        assertEquals(6, otp.length)
    }

    @Test
    fun `verifyOtp with valid code returns tokens`() = runTest {
        val email = "user@example.com"
        val otp = authService.requestOtp(email)
        // Register user first
        authService.registerUser(email = email, name = "Test User", workspaceName = "Test Workspace")
        val tokens = authService.verifyOtp(email, otp)
        assertNotNull(tokens)
        assertNotNull(tokens.accessToken)
        assertNotNull(tokens.refreshToken)
    }
}
```

- [ ] **Step 2: Run test — убедиться что падает**

```bash
cd backend && ./gradlew test --tests "com.appdist.service.AuthServiceTest" 2>&1 | tail -30
```

Expected: FAIL — compilation error.

- [ ] **Step 3: Repository interfaces**

`backend/src/main/kotlin/com/appdist/domain/repository/UserRepository.kt`:
```kotlin
package com.appdist.domain.repository

import com.appdist.domain.model.User
import com.appdist.domain.model.UserRole
import java.util.UUID

interface UserRepository {
    suspend fun findById(id: UUID): User?
    suspend fun findByEmail(email: String): User?
    suspend fun create(
        workspaceId: UUID, email: String, name: String, role: UserRole
    ): User
    suspend fun updateFcmToken(userId: UUID, token: String?)
    suspend fun update(userId: UUID, name: String): User
}
```

`backend/src/main/kotlin/com/appdist/domain/repository/WorkspaceRepository.kt`:
```kotlin
package com.appdist.domain.repository

import com.appdist.domain.model.Workspace
import java.util.UUID

interface WorkspaceRepository {
    suspend fun create(name: String, slug: String, ownerId: UUID): Workspace
    suspend fun findById(id: UUID): Workspace?
    suspend fun findBySlug(slug: String): Workspace?
    suspend fun listAll(): List<Workspace>
}
```

`backend/src/main/kotlin/com/appdist/domain/repository/OtpRepository.kt`:
```kotlin
package com.appdist.domain.repository

import com.appdist.domain.model.OtpCode

interface OtpRepository {
    suspend fun create(email: String, code: String, ttlMinutes: Long): OtpCode
    suspend fun findValid(email: String, code: String): OtpCode?
    suspend fun markUsed(email: String, code: String)
    suspend fun deleteExpired()
}
```

`backend/src/main/kotlin/com/appdist/domain/repository/RefreshTokenRepository.kt`:
```kotlin
package com.appdist.domain.repository

import com.appdist.domain.model.RefreshToken
import java.util.UUID

interface RefreshTokenRepository {
    suspend fun create(userId: UUID, token: String, ttlDays: Long): RefreshToken
    suspend fun findValid(token: String): RefreshToken?
    suspend fun revoke(token: String)
    suspend fun revokeAllForUser(userId: UUID)
}
```

`backend/src/main/kotlin/com/appdist/domain/repository/BuildRepository.kt`:
```kotlin
package com.appdist.domain.repository

import com.appdist.domain.model.Build
import com.appdist.domain.model.BuildStatus
import com.appdist.domain.model.ReleaseChannel
import java.util.UUID

interface BuildRepository {
    suspend fun create(build: Build): Build
    suspend fun findById(id: UUID): Build?
    suspend fun listByProject(
        projectId: UUID,
        channel: ReleaseChannel? = null,
        search: String? = null,
        page: Int = 0,
        limit: Int = 20,
    ): List<Build>
    suspend fun listRecent(workspaceId: UUID, limit: Int = 20): List<Build>
    suspend fun update(buildId: UUID, changelog: String?, status: BuildStatus): Build
    suspend fun delete(buildId: UUID)
    suspend fun findByChecksum(checksum: String): Build?
    suspend fun unsetLatestInChannel(projectId: UUID, channel: ReleaseChannel)
}
```

`backend/src/main/kotlin/com/appdist/domain/repository/AuditRepository.kt`:
```kotlin
package com.appdist.domain.repository

import com.appdist.domain.model.AuditLog
import java.util.UUID

interface AuditRepository {
    suspend fun log(
        userId: UUID,
        action: String,
        resourceType: String,
        resourceId: UUID? = null,
        metadata: Map<String, String>? = null,
    )
    suspend fun list(resourceType: String? = null, page: Int = 0, limit: Int = 50): List<AuditLog>
}
```

- [ ] **Step 4: UserRepositoryImpl**

`backend/src/main/kotlin/com/appdist/infrastructure/database/repository/UserRepositoryImpl.kt`:
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

    override suspend fun create(workspaceId: UUID, email: String, name: String, role: UserRole): User {
        val id = UUID.randomUUID()
        val now = Clock.System.now()
        dbQuery {
            UsersTable.insert {
                it[UsersTable.id] = id
                it[UsersTable.workspaceId] = workspaceId
                it[UsersTable.email] = email
                it[UsersTable.name] = name
                it[UsersTable.role] = role.name
                it[UsersTable.fcmToken] = null
                it[UsersTable.createdAt] = now
            }
        }
        return User(id, workspaceId, email, name, role, null, now)
    }

    override suspend fun updateFcmToken(userId: UUID, token: String?) = dbQuery {
        UsersTable.update({ UsersTable.id eq userId }) {
            it[fcmToken] = token
        }
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

- [ ] **Step 5: AuthService**

`backend/src/main/kotlin/com/appdist/domain/service/AuthService.kt`:
```kotlin
package com.appdist.domain.service

import com.appdist.config.AppConfig
import com.appdist.domain.model.UserRole
import com.appdist.domain.repository.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import kotlin.random.Random
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import java.util.Date

data class AuthTokens(val accessToken: String, val refreshToken: String)

class AuthService(
    private val userRepository: UserRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val otpRepository: OtpRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtConfig: AppConfig.JwtConfig,
    private val otpConfig: AppConfig.OtpConfig,
) {
    suspend fun requestOtp(email: String): String {
        val code = generateOtp(otpConfig.length)
        otpRepository.create(email, code, otpConfig.ttlMinutes)
        // TODO: send via email (console log for now)
        println("OTP for $email: $code")
        return code
    }

    suspend fun registerUser(email: String, name: String, workspaceName: String): com.appdist.domain.model.User {
        val slug = workspaceName.lowercase().replace(" ", "-")
        val workspace = workspaceRepository.create(workspaceName, slug, java.util.UUID.randomUUID())
        return userRepository.create(workspace.id, email, name, UserRole.ADMIN)
    }

    suspend fun verifyOtp(email: String, code: String): AuthTokens {
        val otp = otpRepository.findValid(email, code)
            ?: error("Invalid or expired OTP")
        otpRepository.markUsed(email, code)
        var user = userRepository.findByEmail(email)
            ?: error("User not found, register first")
        return issueTokens(user)
    }

    suspend fun refreshToken(token: String): AuthTokens {
        val refreshToken = refreshTokenRepository.findValid(token)
            ?: error("Invalid or expired refresh token")
        refreshTokenRepository.revoke(token)
        val user = userRepository.findById(refreshToken.userId)
            ?: error("User not found")
        return issueTokens(user)
    }

    private suspend fun issueTokens(user: com.appdist.domain.model.User): AuthTokens {
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
        val rawRefreshToken = generateRefreshToken()
        refreshTokenRepository.create(user.id, rawRefreshToken, jwtConfig.refreshTokenTtlDays)
        return AuthTokens(accessToken, rawRefreshToken)
    }

    private fun generateOtp(length: Int) = (0 until length)
        .map { Random.nextInt(10) }
        .joinToString("")

    private fun generateRefreshToken() = java.util.UUID.randomUUID().toString()
}
```

- [ ] **Step 6: Остальные RepositoryImpl — OtpRepositoryImpl, WorkspaceRepositoryImpl, RefreshTokenRepositoryImpl**

`backend/src/main/kotlin/com/appdist/infrastructure/database/repository/OtpRepositoryImpl.kt`:
```kotlin
package com.appdist.infrastructure.database.repository

import com.appdist.domain.model.OtpCode
import com.appdist.domain.repository.OtpRepository
import com.appdist.infrastructure.database.tables.OtpCodesTable
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlin.time.Duration.Companion.minutes

class OtpRepositoryImpl : OtpRepository {
    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    override suspend fun create(email: String, code: String, ttlMinutes: Long): OtpCode {
        val expiresAt = Clock.System.now() + ttlMinutes.minutes
        dbQuery {
            OtpCodesTable.deleteWhere { OtpCodesTable.email eq email }
            OtpCodesTable.insert {
                it[OtpCodesTable.email] = email
                it[OtpCodesTable.code] = code
                it[OtpCodesTable.expiresAt] = expiresAt
                it[OtpCodesTable.used] = false
            }
        }
        return OtpCode(email, code, expiresAt, false)
    }

    override suspend fun findValid(email: String, code: String): OtpCode? = dbQuery {
        val now = Clock.System.now()
        OtpCodesTable.selectAll()
            .where { (OtpCodesTable.email eq email) and (OtpCodesTable.code eq code) and
                     (OtpCodesTable.used eq false) and (OtpCodesTable.expiresAt greater now) }
            .singleOrNull()?.let {
                OtpCode(it[OtpCodesTable.email], it[OtpCodesTable.code],
                        it[OtpCodesTable.expiresAt], it[OtpCodesTable.used])
            }
    }

    override suspend fun markUsed(email: String, code: String) = dbQuery {
        OtpCodesTable.update({ (OtpCodesTable.email eq email) and (OtpCodesTable.code eq code) }) {
            it[used] = true
        }
        Unit
    }

    override suspend fun deleteExpired() = dbQuery {
        OtpCodesTable.deleteWhere { expiresAt less Clock.System.now() }
        Unit
    }
}
```

`backend/src/main/kotlin/com/appdist/infrastructure/database/repository/WorkspaceRepositoryImpl.kt`:
```kotlin
package com.appdist.infrastructure.database.repository

import com.appdist.domain.model.Workspace
import com.appdist.domain.repository.WorkspaceRepository
import com.appdist.infrastructure.database.tables.WorkspacesTable
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class WorkspaceRepositoryImpl : WorkspaceRepository {
    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    override suspend fun create(name: String, slug: String, ownerId: UUID): Workspace {
        val id = UUID.randomUUID()
        val now = Clock.System.now()
        dbQuery {
            WorkspacesTable.insert {
                it[WorkspacesTable.id] = id
                it[WorkspacesTable.name] = name
                it[WorkspacesTable.slug] = slug
                it[WorkspacesTable.ownerId] = ownerId
                it[WorkspacesTable.createdAt] = now
            }
        }
        return Workspace(id, name, slug, ownerId, now)
    }

    override suspend fun findById(id: UUID): Workspace? = dbQuery {
        WorkspacesTable.selectAll().where { WorkspacesTable.id eq id }
            .singleOrNull()?.toWorkspace()
    }

    override suspend fun findBySlug(slug: String): Workspace? = dbQuery {
        WorkspacesTable.selectAll().where { WorkspacesTable.slug eq slug }
            .singleOrNull()?.toWorkspace()
    }

    override suspend fun listAll(): List<Workspace> = dbQuery {
        WorkspacesTable.selectAll().map { it.toWorkspace() }
    }

    private fun ResultRow.toWorkspace() = Workspace(
        id = this[WorkspacesTable.id],
        name = this[WorkspacesTable.name],
        slug = this[WorkspacesTable.slug],
        ownerId = this[WorkspacesTable.ownerId],
        createdAt = this[WorkspacesTable.createdAt],
    )
}
```

`backend/src/main/kotlin/com/appdist/infrastructure/database/repository/RefreshTokenRepositoryImpl.kt`:
```kotlin
package com.appdist.infrastructure.database.repository

import com.appdist.domain.model.RefreshToken
import com.appdist.domain.repository.RefreshTokenRepository
import com.appdist.infrastructure.database.tables.RefreshTokensTable
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID
import kotlin.time.Duration.Companion.days

class RefreshTokenRepositoryImpl : RefreshTokenRepository {
    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    override suspend fun create(userId: UUID, token: String, ttlDays: Long): RefreshToken {
        val id = UUID.randomUUID()
        val expiresAt = Clock.System.now() + ttlDays.days
        dbQuery {
            RefreshTokensTable.insert {
                it[RefreshTokensTable.id] = id
                it[RefreshTokensTable.userId] = userId
                it[RefreshTokensTable.token] = token
                it[RefreshTokensTable.expiresAt] = expiresAt
                it[RefreshTokensTable.revoked] = false
            }
        }
        return RefreshToken(id, userId, token, expiresAt, false)
    }

    override suspend fun findValid(token: String): RefreshToken? = dbQuery {
        val now = Clock.System.now()
        RefreshTokensTable.selectAll()
            .where { (RefreshTokensTable.token eq token) and
                     (RefreshTokensTable.revoked eq false) and
                     (RefreshTokensTable.expiresAt greater now) }
            .singleOrNull()?.let {
                RefreshToken(
                    it[RefreshTokensTable.id], it[RefreshTokensTable.userId],
                    it[RefreshTokensTable.token], it[RefreshTokensTable.expiresAt],
                    it[RefreshTokensTable.revoked]
                )
            }
    }

    override suspend fun revoke(token: String) = dbQuery {
        RefreshTokensTable.update({ RefreshTokensTable.token eq token }) {
            it[revoked] = true
        }
        Unit
    }

    override suspend fun revokeAllForUser(userId: UUID) = dbQuery {
        RefreshTokensTable.update({ RefreshTokensTable.userId eq userId }) {
            it[revoked] = true
        }
        Unit
    }
}
```

- [ ] **Step 7: Run AuthService tests**

```bash
cd backend && ./gradlew test --tests "com.appdist.service.AuthServiceTest"
```

Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add backend/src/
git commit -m "feat: add repositories, AuthService with OTP + JWT tokens"
```

---

## Task 6: Auth Routes

**Files:**
- Create: `backend/src/main/kotlin/com/appdist/api/dto/AuthDtos.kt`
- Create: `backend/src/main/kotlin/com/appdist/api/routes/AuthRoutes.kt`
- Create: `backend/src/main/kotlin/com/appdist/plugins/Authentication.kt`
- Create: `backend/src/main/kotlin/com/appdist/plugins/Routing.kt`
- Create: `backend/src/test/kotlin/com/appdist/api/AuthRoutesTest.kt`

- [ ] **Step 1: Auth DTOs**

`backend/src/main/kotlin/com/appdist/api/dto/AuthDtos.kt`:
```kotlin
package com.appdist.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class RequestOtpRequest(val email: String)

@Serializable
data class VerifyOtpRequest(val email: String, val otp: String)

@Serializable
data class RegisterRequest(val email: String, val name: String, val workspaceName: String)

@Serializable
data class RefreshTokenRequest(val refreshToken: String)

@Serializable
data class AuthResponse(val accessToken: String, val refreshToken: String)

@Serializable
data class MessageResponse(val message: String)
```

- [ ] **Step 2: Auth Routes**

`backend/src/main/kotlin/com/appdist/api/routes/AuthRoutes.kt`:
```kotlin
package com.appdist.api.routes

import com.appdist.api.dto.*
import com.appdist.domain.service.AuthService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(authService: AuthService) {
    route("/auth") {
        post("/request-otp") {
            val req = call.receive<RequestOtpRequest>()
            if (req.email.isBlank() || !req.email.contains("@")) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_EMAIL", "Valid email required"))
                return@post
            }
            authService.requestOtp(req.email)
            call.respond(HttpStatusCode.OK, MessageResponse("OTP sent to ${req.email}"))
        }

        post("/register") {
            val req = call.receive<RegisterRequest>()
            val user = authService.registerUser(req.email, req.name, req.workspaceName)
            call.respond(HttpStatusCode.Created, MessageResponse("User ${user.email} registered"))
        }

        post("/verify-otp") {
            val req = call.receive<VerifyOtpRequest>()
            val tokens = runCatching { authService.verifyOtp(req.email, req.otp) }
                .getOrElse {
                    call.respond(HttpStatusCode.Unauthorized,
                        ErrorResponse("INVALID_OTP", it.message ?: "Invalid OTP"))
                    return@post
                }
            call.respond(HttpStatusCode.OK, AuthResponse(tokens.accessToken, tokens.refreshToken))
        }

        post("/refresh") {
            val req = call.receive<RefreshTokenRequest>()
            val tokens = runCatching { authService.refreshToken(req.refreshToken) }
                .getOrElse {
                    call.respond(HttpStatusCode.Unauthorized,
                        ErrorResponse("INVALID_TOKEN", "Invalid refresh token"))
                    return@post
                }
            call.respond(HttpStatusCode.OK, AuthResponse(tokens.accessToken, tokens.refreshToken))
        }
    }
}
```

- [ ] **Step 3: Authentication plugin**

`backend/src/main/kotlin/com/appdist/plugins/Authentication.kt`:
```kotlin
package com.appdist.plugins

import com.appdist.config.AppConfig
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

const val JWT_AUTH = "jwt-auth"

data class AuthPrincipal(
    val userId: String,
    val email: String,
    val role: String,
    val workspaceId: String,
)

fun Application.configureAuth(jwtConfig: AppConfig.JwtConfig) {
    install(Authentication) {
        jwt(JWT_AUTH) {
            val algorithm = Algorithm.HMAC256(jwtConfig.secret)
            verifier(
                JWT.require(algorithm)
                    .withIssuer(jwtConfig.issuer)
                    .withAudience(jwtConfig.audience)
                    .build()
            )
            validate { credential ->
                val userId = credential.payload.subject ?: return@validate null
                val email = credential.payload.getClaim("email").asString() ?: return@validate null
                val role = credential.payload.getClaim("role").asString() ?: return@validate null
                val workspaceId = credential.payload.getClaim("workspace_id").asString() ?: return@validate null
                AuthPrincipal(userId, email, role, workspaceId)
            }
            challenge { _, _ ->
                call.respond(
                    io.ktor.http.HttpStatusCode.Unauthorized,
                    com.appdist.api.dto.ErrorResponse("UNAUTHORIZED", "Token is not valid or expired")
                )
            }
        }
    }
}
```

- [ ] **Step 4: Routing plugin**

`backend/src/main/kotlin/com/appdist/plugins/Routing.kt`:
```kotlin
package com.appdist.plugins

import com.appdist.api.routes.*
import com.appdist.config.AppConfig
import com.appdist.domain.service.AuthService
import com.appdist.infrastructure.database.repository.*
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting(config: AppConfig) {
    // Wire up dependencies manually (replace with DI later)
    val userRepo = UserRepositoryImpl()
    val workspaceRepo = WorkspaceRepositoryImpl()
    val otpRepo = OtpRepositoryImpl()
    val refreshTokenRepo = RefreshTokenRepositoryImpl()

    val authService = AuthService(
        userRepository = userRepo,
        workspaceRepository = workspaceRepo,
        otpRepository = otpRepo,
        refreshTokenRepository = refreshTokenRepo,
        jwtConfig = config.jwt,
        otpConfig = config.otp,
    )

    routing {
        route("/api/v1") {
            authRoutes(authService)
            // more routes will be added in next tasks
        }
    }
}
```

- [ ] **Step 5: Написать интеграционный тест для auth routes**

`backend/src/test/kotlin/com/appdist/api/AuthRoutesTest.kt`:
```kotlin
package com.appdist.api

import com.appdist.TestDatabase
import com.appdist.api.dto.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AuthRoutesTest {
    @Test
    fun `request OTP returns 200 for valid email`() = testApplication {
        application { testModule() }
        val client = createClient {
            install(ContentNegotiation) { json() }
        }
        val response = client.post("/api/v1/auth/request-otp") {
            contentType(ContentType.Application.Json)
            setBody(RequestOtpRequest("user@example.com"))
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `request OTP returns 400 for invalid email`() = testApplication {
        application { testModule() }
        val client = createClient {
            install(ContentNegotiation) { json() }
        }
        val response = client.post("/api/v1/auth/request-otp") {
            contentType(ContentType.Application.Json)
            setBody(RequestOtpRequest("notanemail"))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
```

`backend/src/test/kotlin/com/appdist/TestApplication.kt`:
```kotlin
package com.appdist

import com.appdist.config.AppConfig
import com.appdist.infrastructure.database.repository.*
import com.appdist.domain.service.AuthService
import com.appdist.plugins.*
import com.appdist.api.routes.*
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.testModule() {
    TestDatabase.init()
    configureSerialization()
    configureStatusPages()

    val jwtConfig = AppConfig.JwtConfig("test-secret-key-for-testing-only-32chars", "appdist", "appdist-client", 60L, 30L)
    val otpConfig = AppConfig.OtpConfig(5L, 6)

    val authService = AuthService(
        UserRepositoryImpl(), WorkspaceRepositoryImpl(),
        OtpRepositoryImpl(), RefreshTokenRepositoryImpl(),
        jwtConfig, otpConfig
    )
    configureAuth(jwtConfig)

    routing {
        route("/api/v1") {
            authRoutes(authService)
        }
    }
}
```

- [ ] **Step 6: Run auth route tests**

```bash
cd backend && ./gradlew test --tests "com.appdist.api.AuthRoutesTest"
```

Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add backend/src/
git commit -m "feat: add auth API routes (request-otp, register, verify-otp, refresh)"
```

---

## Task 7: MinIO Storage Client

**Files:**
- Create: `backend/src/main/kotlin/com/appdist/infrastructure/storage/StorageClient.kt`
- Create: `backend/src/main/kotlin/com/appdist/infrastructure/storage/MinioStorageClient.kt`
- Create: `backend/src/main/kotlin/com/appdist/domain/service/StorageService.kt`

- [ ] **Step 1: StorageClient interface**

```kotlin
package com.appdist.infrastructure.storage

import java.io.InputStream

interface StorageClient {
    fun upload(key: String, inputStream: InputStream, size: Long, contentType: String)
    fun generateDownloadUrl(key: String, ttlMinutes: Long): String
    fun delete(key: String)
    fun ensureBucketExists(bucket: String)
}
```

- [ ] **Step 2: MinioStorageClient**

```kotlin
package com.appdist.infrastructure.storage

import com.appdist.config.AppConfig
import io.minio.*
import io.minio.http.Method
import java.io.InputStream
import java.util.concurrent.TimeUnit

class MinioStorageClient(private val config: AppConfig.StorageConfig) : StorageClient {
    private val client = MinioClient.builder()
        .endpoint(config.endpoint)
        .credentials(config.accessKey, config.secretKey)
        .build()

    init {
        ensureBucketExists(config.bucket)
    }

    override fun ensureBucketExists(bucket: String) {
        if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build())
        }
    }

    override fun upload(key: String, inputStream: InputStream, size: Long, contentType: String) {
        client.putObject(
            PutObjectArgs.builder()
                .bucket(config.bucket)
                .`object`(key)
                .stream(inputStream, size, -1)
                .contentType(contentType)
                .build()
        )
    }

    override fun generateDownloadUrl(key: String, ttlMinutes: Long): String =
        client.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(config.bucket)
                .`object`(key)
                .expiry(ttlMinutes.toInt(), TimeUnit.MINUTES)
                .build()
        )

    override fun delete(key: String) {
        client.removeObject(
            RemoveObjectArgs.builder()
                .bucket(config.bucket)
                .`object`(key)
                .build()
        )
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/kotlin/com/appdist/infrastructure/storage/
git commit -m "feat: add MinIO storage client with signed URL generation"
```

---

## Task 8: APK Metadata Extractor

**Files:**
- Create: `backend/src/main/kotlin/com/appdist/infrastructure/apk/ApkMetadataExtractor.kt`

- [ ] **Step 1: ApkMetadataExtractor**

```kotlin
package com.appdist.infrastructure.apk

import net.dongliu.apk.parser.ApkFile
import java.io.File
import java.security.MessageDigest

data class ApkMetadata(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val abis: List<String>,
    val certFingerprint: String?,
    val fileSizeBytes: Long,
    val checksumSha256: String,
)

object ApkMetadataExtractor {
    fun extract(file: File): ApkMetadata {
        val apkFile = ApkFile(file)
        val info = apkFile.apkMeta

        val sha256 = file.inputStream().use { input ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }

        val certFingerprint = runCatching {
            apkFile.apkSingers.firstOrNull()
                ?.certificateMetas?.firstOrNull()
                ?.certMd5
        }.getOrNull()

        val abis = apkFile.apkMeta.abis?.toList() ?: emptyList()

        apkFile.close()

        return ApkMetadata(
            packageName = info.packageName,
            versionName = info.versionName ?: "unknown",
            versionCode = info.versionCode ?: 0L,
            minSdk = info.minSdkVersion?.toIntOrNull() ?: 1,
            targetSdk = info.targetSdkVersion?.toIntOrNull() ?: 1,
            abis = abis,
            certFingerprint = certFingerprint,
            fileSizeBytes = file.length(),
            checksumSha256 = sha256,
        )
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/kotlin/com/appdist/infrastructure/apk/
git commit -m "feat: add APK metadata extractor (package, version, SDK, checksum)"
```

---

## Task 9: Build Service + Build Repository

**Files:**
- Create: `backend/src/main/kotlin/com/appdist/infrastructure/database/repository/BuildRepositoryImpl.kt`
- Create: `backend/src/main/kotlin/com/appdist/infrastructure/database/repository/AuditRepositoryImpl.kt`
- Create: `backend/src/main/kotlin/com/appdist/domain/service/BuildService.kt`

- [ ] **Step 1: BuildRepositoryImpl**

```kotlin
package com.appdist.infrastructure.database.repository

import com.appdist.domain.model.*
import com.appdist.domain.repository.BuildRepository
import com.appdist.infrastructure.database.tables.BuildsTable
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class BuildRepositoryImpl : BuildRepository {
    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    override suspend fun create(build: Build): Build = dbQuery {
        BuildsTable.insert { row ->
            row[id] = build.id
            row[projectId] = build.projectId
            row[versionName] = build.versionName
            row[versionCode] = build.versionCode
            row[buildNumber] = build.buildNumber
            row[flavor] = build.flavor
            row[buildType] = build.buildType
            row[environment] = build.environment.name
            row[channel] = build.channel.name
            row[branch] = build.branch
            row[commitHash] = build.commitHash
            row[uploaderId] = build.uploaderId
            row[uploadDate] = build.uploadDate
            row[changelog] = build.changelog
            row[fileSizeBytes] = build.fileSizeBytes
            row[checksumSha256] = build.checksumSha256
            row[minSdk] = build.minSdk
            row[targetSdk] = build.targetSdk
            row[certFingerprint] = build.certFingerprint
            row[abis] = Json.encodeToString(build.abis)
            row[storageKey] = build.storageKey
            row[status] = build.status.name
            row[expiryDate] = build.expiryDate
            row[isLatestInChannel] = build.isLatestInChannel
        }
        build
    }

    override suspend fun findById(id: UUID): Build? = dbQuery {
        BuildsTable.selectAll().where { BuildsTable.id eq id }.singleOrNull()?.toBuild()
    }

    override suspend fun listByProject(
        projectId: UUID, channel: ReleaseChannel?, search: String?, page: Int, limit: Int
    ): List<Build> = dbQuery {
        BuildsTable.selectAll()
            .where {
                var condition = BuildsTable.projectId eq projectId
                if (channel != null) condition = condition and (BuildsTable.channel eq channel.name)
                if (search != null) condition = condition and (
                    (BuildsTable.versionName like "%$search%") or
                    (BuildsTable.branch like "%$search%")
                )
                condition
            }
            .orderBy(BuildsTable.uploadDate, SortOrder.DESC)
            .limit(limit, offset = (page * limit).toLong())
            .map { it.toBuild() }
    }

    override suspend fun listRecent(workspaceId: UUID, limit: Int): List<Build> = dbQuery {
        // join with projects to filter by workspace — simplified for now
        BuildsTable.selectAll()
            .orderBy(BuildsTable.uploadDate, SortOrder.DESC)
            .limit(limit)
            .map { it.toBuild() }
    }

    override suspend fun update(buildId: UUID, changelog: String?, status: BuildStatus): Build = dbQuery {
        BuildsTable.update({ BuildsTable.id eq buildId }) {
            if (changelog != null) it[BuildsTable.changelog] = changelog
            it[BuildsTable.status] = status.name
        }
        BuildsTable.selectAll().where { BuildsTable.id eq buildId }.single().toBuild()
    }

    override suspend fun delete(buildId: UUID) = dbQuery {
        BuildsTable.deleteWhere { id eq buildId }
        Unit
    }

    override suspend fun findByChecksum(checksum: String): Build? = dbQuery {
        BuildsTable.selectAll().where { BuildsTable.checksumSha256 eq checksum }.singleOrNull()?.toBuild()
    }

    override suspend fun unsetLatestInChannel(projectId: UUID, channel: ReleaseChannel) = dbQuery {
        BuildsTable.update({
            (BuildsTable.projectId eq projectId) and (BuildsTable.channel eq channel.name)
        }) { it[isLatestInChannel] = false }
        Unit
    }

    private fun ResultRow.toBuild() = Build(
        id = this[BuildsTable.id],
        projectId = this[BuildsTable.projectId],
        versionName = this[BuildsTable.versionName],
        versionCode = this[BuildsTable.versionCode],
        buildNumber = this[BuildsTable.buildNumber],
        flavor = this[BuildsTable.flavor],
        buildType = this[BuildsTable.buildType],
        environment = BuildEnvironment.valueOf(this[BuildsTable.environment]),
        channel = ReleaseChannel.valueOf(this[BuildsTable.channel]),
        branch = this[BuildsTable.branch],
        commitHash = this[BuildsTable.commitHash],
        uploaderId = this[BuildsTable.uploaderId],
        uploadDate = this[BuildsTable.uploadDate],
        changelog = this[BuildsTable.changelog],
        fileSizeBytes = this[BuildsTable.fileSizeBytes],
        checksumSha256 = this[BuildsTable.checksumSha256],
        minSdk = this[BuildsTable.minSdk],
        targetSdk = this[BuildsTable.targetSdk],
        certFingerprint = this[BuildsTable.certFingerprint],
        abis = runCatching { Json.decodeFromString<List<String>>(this[BuildsTable.abis]) }.getOrDefault(emptyList()),
        storageKey = this[BuildsTable.storageKey],
        status = BuildStatus.valueOf(this[BuildsTable.status]),
        expiryDate = this[BuildsTable.expiryDate],
        isLatestInChannel = this[BuildsTable.isLatestInChannel],
    )
}
```

- [ ] **Step 2: AuditRepositoryImpl**

```kotlin
package com.appdist.infrastructure.database.repository

import com.appdist.domain.model.AuditLog
import com.appdist.domain.repository.AuditRepository
import com.appdist.infrastructure.database.tables.AuditLogsTable
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

class AuditRepositoryImpl : AuditRepository {
    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    override suspend fun log(
        userId: UUID, action: String, resourceType: String,
        resourceId: UUID?, metadata: Map<String, String>?
    ) = dbQuery {
        AuditLogsTable.insert {
            it[id] = UUID.randomUUID()
            it[AuditLogsTable.userId] = userId
            it[AuditLogsTable.action] = action
            it[AuditLogsTable.resourceType] = resourceType
            it[AuditLogsTable.resourceId] = resourceId
            it[AuditLogsTable.metadata] = metadata?.let { m -> Json.encodeToString(m) }
            it[createdAt] = Clock.System.now()
        }
        Unit
    }

    override suspend fun list(resourceType: String?, page: Int, limit: Int): List<AuditLog> = dbQuery {
        AuditLogsTable.selectAll()
            .apply { if (resourceType != null) where { AuditLogsTable.resourceType eq resourceType } }
            .orderBy(AuditLogsTable.createdAt, SortOrder.DESC)
            .limit(limit, offset = (page * limit).toLong())
            .map {
                AuditLog(
                    id = it[AuditLogsTable.id],
                    userId = it[AuditLogsTable.userId],
                    action = it[AuditLogsTable.action],
                    resourceType = it[AuditLogsTable.resourceType],
                    resourceId = it[AuditLogsTable.resourceId],
                    metadata = it[AuditLogsTable.metadata]?.let { json ->
                        runCatching { Json.decodeFromString<Map<String, String>>(json) }.getOrNull()
                    },
                    createdAt = it[AuditLogsTable.createdAt],
                )
            }
    }
}
```

- [ ] **Step 3: BuildService**

```kotlin
package com.appdist.domain.service

import com.appdist.domain.model.*
import com.appdist.domain.repository.*
import com.appdist.infrastructure.apk.ApkMetadataExtractor
import com.appdist.infrastructure.storage.StorageClient
import com.appdist.config.AppConfig
import kotlinx.datetime.Clock
import java.io.File
import java.util.UUID

data class UploadRequest(
    val projectId: UUID,
    val uploaderId: UUID,
    val environment: BuildEnvironment,
    val channel: ReleaseChannel,
    val buildType: String,
    val flavor: String?,
    val branch: String?,
    val commitHash: String?,
    val changelog: String?,
    val file: File,
    val originalFileName: String,
)

class BuildService(
    private val buildRepository: BuildRepository,
    private val storageClient: StorageClient,
    private val auditRepository: AuditRepository,
    private val storageConfig: AppConfig.StorageConfig,
) {
    suspend fun uploadBuild(request: UploadRequest): Build {
        val metadata = ApkMetadataExtractor.extract(request.file)

        // Check for duplicate
        val existing = buildRepository.findByChecksum(metadata.checksumSha256)
        if (existing != null) {
            error("Build with checksum ${metadata.checksumSha256} already exists (id: ${existing.id})")
        }

        val buildId = UUID.randomUUID()
        val storageKey = "${request.projectId}/${buildId}/app.apk"

        storageClient.upload(
            key = storageKey,
            inputStream = request.file.inputStream(),
            size = metadata.fileSizeBytes,
            contentType = "application/vnd.android.package-archive"
        )

        // Unset previous latest in channel
        buildRepository.unsetLatestInChannel(request.projectId, request.channel)

        val build = Build(
            id = buildId,
            projectId = request.projectId,
            versionName = metadata.versionName,
            versionCode = metadata.versionCode,
            buildNumber = null,
            flavor = request.flavor,
            buildType = request.buildType,
            environment = request.environment,
            channel = request.channel,
            branch = request.branch,
            commitHash = request.commitHash,
            uploaderId = request.uploaderId,
            uploadDate = Clock.System.now(),
            changelog = request.changelog,
            fileSizeBytes = metadata.fileSizeBytes,
            checksumSha256 = metadata.checksumSha256,
            minSdk = metadata.minSdk,
            targetSdk = metadata.targetSdk,
            certFingerprint = metadata.certFingerprint,
            abis = metadata.abis,
            storageKey = storageKey,
            status = BuildStatus.ACTIVE,
            expiryDate = null,
            isLatestInChannel = true,
        )

        val saved = buildRepository.create(build)
        auditRepository.log(
            userId = request.uploaderId,
            action = "build.upload",
            resourceType = "build",
            resourceId = buildId,
            metadata = mapOf("version" to metadata.versionName, "channel" to request.channel.name)
        )
        return saved
    }

    suspend fun getDownloadUrl(buildId: UUID, requesterId: UUID): String {
        val build = buildRepository.findById(buildId) ?: error("Build not found")
        val url = storageClient.generateDownloadUrl(build.storageKey, ttlMinutes = 15L)
        auditRepository.log(requesterId, "build.download_url", "build", buildId)
        return url
    }

    suspend fun listBuilds(
        projectId: UUID, channel: ReleaseChannel?, search: String?, page: Int, limit: Int
    ): List<Build> = buildRepository.listByProject(projectId, channel, search, page, limit)

    suspend fun getBuild(buildId: UUID): Build =
        buildRepository.findById(buildId) ?: error("Build not found")

    suspend fun updateBuild(buildId: UUID, changelog: String?, status: BuildStatus, requesterId: UUID): Build {
        val build = buildRepository.update(buildId, changelog, status)
        auditRepository.log(requesterId, "build.update", "build", buildId,
            mapOf("status" to status.name))
        return build
    }

    suspend fun deleteBuild(buildId: UUID, requesterId: UUID) {
        val build = buildRepository.findById(buildId) ?: error("Build not found")
        storageClient.delete(build.storageKey)
        buildRepository.delete(buildId)
        auditRepository.log(requesterId, "build.delete", "build", buildId)
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/
git commit -m "feat: add BuildService, BuildRepositoryImpl, AuditRepositoryImpl"
```

---

## Task 10: Build + Upload Routes

**Files:**
- Create: `backend/src/main/kotlin/com/appdist/api/dto/BuildDtos.kt`
- Create: `backend/src/main/kotlin/com/appdist/api/routes/BuildRoutes.kt`
- Create: `backend/src/main/kotlin/com/appdist/api/routes/UploadRoutes.kt`
- Create: `backend/src/test/kotlin/com/appdist/api/BuildRoutesTest.kt`

- [ ] **Step 1: Build DTOs**

`backend/src/main/kotlin/com/appdist/api/dto/BuildDtos.kt`:
```kotlin
package com.appdist.api.dto

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

@Serializable
data class BuildDto(
    val id: String,
    val projectId: String,
    val versionName: String,
    val versionCode: Long,
    val buildNumber: String?,
    val flavor: String?,
    val buildType: String,
    val environment: String,
    val channel: String,
    val branch: String?,
    val commitHash: String?,
    val uploaderEmail: String?,
    val uploadDate: String,
    val changelog: String?,
    val fileSizeBytes: Long,
    val checksumSha256: String,
    val minSdk: Int,
    val targetSdk: Int,
    val certFingerprint: String?,
    val abis: List<String>,
    val status: String,
    val expiryDate: String?,
    val isLatestInChannel: Boolean,
)

@Serializable
data class BuildListResponse(
    val builds: List<BuildDto>,
    val total: Int,
    val page: Int,
    val limit: Int,
)

@Serializable
data class UpdateBuildRequest(
    val changelog: String? = null,
    val status: String? = null,
)

@Serializable
data class DownloadUrlResponse(val url: String, val expiresInMinutes: Int = 15)
```

- [ ] **Step 2: Build Routes**

`backend/src/main/kotlin/com/appdist/api/routes/BuildRoutes.kt`:
```kotlin
package com.appdist.api.routes

import com.appdist.api.dto.*
import com.appdist.domain.model.BuildStatus
import com.appdist.domain.model.ReleaseChannel
import com.appdist.domain.service.BuildService
import com.appdist.plugins.AuthPrincipal
import com.appdist.plugins.JWT_AUTH
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.buildRoutes(buildService: BuildService) {
    authenticate(JWT_AUTH) {
        route("/projects/{projectId}/builds") {
            get {
                val projectId = UUID.fromString(call.parameters["projectId"]!!)
                val channel = call.request.queryParameters["channel"]
                    ?.let { runCatching { ReleaseChannel.valueOf(it.uppercase()) }.getOrNull() }
                val search = call.request.queryParameters["search"]
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
                val limit = (call.request.queryParameters["limit"]?.toIntOrNull() ?: 20).coerceIn(1, 100)

                val builds = buildService.listBuilds(projectId, channel, search, page, limit)
                call.respond(BuildListResponse(
                    builds = builds.map { it.toDto() },
                    total = builds.size, page = page, limit = limit
                ))
            }
        }

        route("/builds/{buildId}") {
            get {
                val buildId = UUID.fromString(call.parameters["buildId"]!!)
                val build = runCatching { buildService.getBuild(buildId) }.getOrElse {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", "Build not found"))
                    return@get
                }
                call.respond(build.toDto())
            }

            patch {
                val buildId = UUID.fromString(call.parameters["buildId"]!!)
                val principal = call.principal<AuthPrincipal>()!!
                val req = call.receive<UpdateBuildRequest>()
                val status = req.status?.let { BuildStatus.valueOf(it.uppercase()) } ?: BuildStatus.ACTIVE
                val build = buildService.updateBuild(buildId, req.changelog, status, UUID.fromString(principal.userId))
                call.respond(build.toDto())
            }

            delete {
                val buildId = UUID.fromString(call.parameters["buildId"]!!)
                val principal = call.principal<AuthPrincipal>()!!
                buildService.deleteBuild(buildId, UUID.fromString(principal.userId))
                call.respond(HttpStatusCode.NoContent)
            }

            get("/download-url") {
                val buildId = UUID.fromString(call.parameters["buildId"]!!)
                val principal = call.principal<AuthPrincipal>()!!
                val url = runCatching {
                    buildService.getDownloadUrl(buildId, UUID.fromString(principal.userId))
                }.getOrElse {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", it.message ?: "Build not found"))
                    return@get
                }
                call.respond(DownloadUrlResponse(url))
            }
        }
    }
}

private fun com.appdist.domain.model.Build.toDto() = BuildDto(
    id = id.toString(), projectId = projectId.toString(),
    versionName = versionName, versionCode = versionCode,
    buildNumber = buildNumber, flavor = flavor, buildType = buildType,
    environment = environment.name, channel = channel.name,
    branch = branch, commitHash = commitHash,
    uploaderEmail = null, // TODO: join with users
    uploadDate = uploadDate.toString(), changelog = changelog,
    fileSizeBytes = fileSizeBytes, checksumSha256 = checksumSha256,
    minSdk = minSdk, targetSdk = targetSdk, certFingerprint = certFingerprint,
    abis = abis, status = status.name, expiryDate = expiryDate?.toString(),
    isLatestInChannel = isLatestInChannel,
)
```

- [ ] **Step 3: Upload Route (multipart)**

`backend/src/main/kotlin/com/appdist/api/routes/UploadRoutes.kt`:
```kotlin
package com.appdist.api.routes

import com.appdist.api.dto.ErrorResponse
import com.appdist.domain.model.BuildEnvironment
import com.appdist.domain.model.ReleaseChannel
import com.appdist.domain.service.BuildService
import com.appdist.domain.service.UploadRequest
import com.appdist.plugins.AuthPrincipal
import com.appdist.plugins.JWT_AUTH
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File
import java.util.UUID

fun Route.uploadRoutes(buildService: BuildService) {
    authenticate(JWT_AUTH) {
        post("/builds/upload") {
            val principal = call.principal<AuthPrincipal>()!!
            val multipart = call.receiveMultipart()

            var apkFile: File? = null
            var projectId: UUID? = null
            var environment = BuildEnvironment.QA
            var channel = ReleaseChannel.INTERNAL
            var buildType = "debug"
            var flavor: String? = null
            var branch: String? = null
            var commitHash: String? = null
            var changelog: String? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        when (part.name) {
                            "projectId" -> projectId = UUID.fromString(part.value)
                            "environment" -> environment = BuildEnvironment.valueOf(part.value.uppercase())
                            "channel" -> channel = ReleaseChannel.valueOf(part.value.uppercase())
                            "buildType" -> buildType = part.value
                            "flavor" -> flavor = part.value
                            "branch" -> branch = part.value
                            "commitHash" -> commitHash = part.value
                            "changelog" -> changelog = part.value
                        }
                    }
                    is PartData.FileItem -> {
                        if (part.name == "apk") {
                            val tempFile = File.createTempFile("upload_", ".apk")
                            part.streamProvider().use { input ->
                                tempFile.outputStream().use { output -> input.copyTo(output) }
                            }
                            apkFile = tempFile
                        }
                    }
                    else -> {}
                }
                part.dispose()
            }

            val file = apkFile
            val pid = projectId

            if (file == null || pid == null) {
                call.respond(HttpStatusCode.BadRequest,
                    ErrorResponse("MISSING_FIELDS", "apk file and projectId are required"))
                return@post
            }

            if (!file.name.endsWith(".apk") && file.length() < 1000) {
                file.delete()
                call.respond(HttpStatusCode.BadRequest,
                    ErrorResponse("INVALID_FILE", "Invalid APK file"))
                return@post
            }

            val build = runCatching {
                buildService.uploadBuild(
                    UploadRequest(pid, UUID.fromString(principal.userId),
                        environment, channel, buildType, flavor, branch,
                        commitHash, changelog, file, "app.apk")
                )
            }.also { file.delete() }.getOrElse {
                call.respond(HttpStatusCode.InternalServerError,
                    ErrorResponse("UPLOAD_FAILED", it.message ?: "Upload failed"))
                return@post
            }

            call.respond(HttpStatusCode.Created, build.toDto())
        }
    }
}

private fun com.appdist.domain.model.Build.toDto() = com.appdist.api.dto.BuildDto(
    id = id.toString(), projectId = projectId.toString(),
    versionName = versionName, versionCode = versionCode,
    buildNumber = buildNumber, flavor = flavor, buildType = buildType,
    environment = environment.name, channel = channel.name,
    branch = branch, commitHash = commitHash, uploaderEmail = null,
    uploadDate = uploadDate.toString(), changelog = changelog,
    fileSizeBytes = fileSizeBytes, checksumSha256 = checksumSha256,
    minSdk = minSdk, targetSdk = targetSdk, certFingerprint = certFingerprint,
    abis = abis, status = status.name, expiryDate = expiryDate?.toString(),
    isLatestInChannel = isLatestInChannel,
)
```

- [ ] **Step 4: Добавить build routes в Routing.kt**

Обновить `configureRouting` в Routing.kt, добавив:
```kotlin
val buildRepo = BuildRepositoryImpl()
val auditRepo = AuditRepositoryImpl()
val storageClient = MinioStorageClient(config.storage)
val buildService = BuildService(buildRepo, storageClient, auditRepo, config.storage)

routing {
    route("/api/v1") {
        authRoutes(authService)
        buildRoutes(buildService)
        uploadRoutes(buildService)
    }
}
```

- [ ] **Step 5: Запустить full build тест**

```bash
cd backend && ./gradlew test 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL, все тесты PASS

- [ ] **Step 6: Commit**

```bash
git add backend/src/
git commit -m "feat: add build CRUD routes and multipart APK upload endpoint"
```

---

## Task 11: Project Routes + User Routes

**Files:**
- Create: `backend/src/main/kotlin/com/appdist/api/dto/ProjectDtos.kt`
- Create: `backend/src/main/kotlin/com/appdist/api/routes/ProjectRoutes.kt`
- Create: `backend/src/main/kotlin/com/appdist/api/routes/UserRoutes.kt`
- Create: `backend/src/main/kotlin/com/appdist/domain/service/ProjectService.kt`
- Create: `backend/src/main/kotlin/com/appdist/infrastructure/database/repository/ProjectRepositoryImpl.kt`

- [ ] **Step 1: Project DTOs**

```kotlin
package com.appdist.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateProjectRequest(val name: String, val packageName: String)

@Serializable
data class ProjectDto(
    val id: String,
    val workspaceId: String,
    val name: String,
    val packageName: String,
    val iconUrl: String?,
    val createdAt: String,
)

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val name: String,
    val role: String,
    val workspaceId: String,
)

@Serializable
data class UpdateProfileRequest(val name: String? = null, val fcmToken: String? = null)
```

- [ ] **Step 2: ProjectRepositoryImpl**

```kotlin
package com.appdist.infrastructure.database.repository

import com.appdist.domain.model.Project
import com.appdist.domain.repository.ProjectRepository
import com.appdist.infrastructure.database.tables.ProjectsTable
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

interface ProjectRepository {
    suspend fun create(workspaceId: UUID, name: String, packageName: String): Project
    suspend fun findById(id: UUID): Project?
    suspend fun listByWorkspace(workspaceId: UUID): List<Project>
    suspend fun delete(id: UUID)
}

class ProjectRepositoryImpl : ProjectRepository {
    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    override suspend fun create(workspaceId: UUID, name: String, packageName: String): Project {
        val id = UUID.randomUUID()
        val now = Clock.System.now()
        dbQuery {
            ProjectsTable.insert {
                it[ProjectsTable.id] = id
                it[ProjectsTable.workspaceId] = workspaceId
                it[ProjectsTable.name] = name
                it[ProjectsTable.packageName] = packageName
                it[iconUrl] = null
                it[createdAt] = now
            }
        }
        return Project(id, workspaceId, name, packageName, null, now)
    }

    override suspend fun findById(id: UUID): Project? = dbQuery {
        ProjectsTable.selectAll().where { ProjectsTable.id eq id }.singleOrNull()?.toProject()
    }

    override suspend fun listByWorkspace(workspaceId: UUID): List<Project> = dbQuery {
        ProjectsTable.selectAll().where { ProjectsTable.workspaceId eq workspaceId }
            .orderBy(ProjectsTable.createdAt, SortOrder.DESC).map { it.toProject() }
    }

    override suspend fun delete(id: UUID) = dbQuery {
        ProjectsTable.deleteWhere { ProjectsTable.id eq id }
        Unit
    }

    private fun ResultRow.toProject() = Project(
        id = this[ProjectsTable.id], workspaceId = this[ProjectsTable.workspaceId],
        name = this[ProjectsTable.name], packageName = this[ProjectsTable.packageName],
        iconUrl = this[ProjectsTable.iconUrl], createdAt = this[ProjectsTable.createdAt],
    )
}
```

- [ ] **Step 3: Project Routes**

```kotlin
package com.appdist.api.routes

import com.appdist.api.dto.*
import com.appdist.infrastructure.database.repository.ProjectRepositoryImpl
import com.appdist.plugins.AuthPrincipal
import com.appdist.plugins.JWT_AUTH
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.projectRoutes(projectRepo: ProjectRepositoryImpl) {
    authenticate(JWT_AUTH) {
        route("/workspaces/{workspaceId}/projects") {
            get {
                val workspaceId = UUID.fromString(call.parameters["workspaceId"]!!)
                val projects = projectRepo.listByWorkspace(workspaceId)
                call.respond(projects.map {
                    ProjectDto(it.id.toString(), it.workspaceId.toString(),
                        it.name, it.packageName, it.iconUrl, it.createdAt.toString())
                })
            }
            post {
                val workspaceId = UUID.fromString(call.parameters["workspaceId"]!!)
                val req = call.receive<CreateProjectRequest>()
                val project = projectRepo.create(workspaceId, req.name, req.packageName)
                call.respond(HttpStatusCode.Created,
                    ProjectDto(project.id.toString(), project.workspaceId.toString(),
                        project.name, project.packageName, project.iconUrl, project.createdAt.toString()))
            }
        }

        route("/projects/{projectId}") {
            get {
                val id = UUID.fromString(call.parameters["projectId"]!!)
                val project = projectRepo.findById(id)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", "Project not found"))
                call.respond(ProjectDto(project.id.toString(), project.workspaceId.toString(),
                    project.name, project.packageName, project.iconUrl, project.createdAt.toString()))
            }
            delete {
                val id = UUID.fromString(call.parameters["projectId"]!!)
                projectRepo.delete(id)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
```

- [ ] **Step 4: User Routes**

```kotlin
package com.appdist.api.routes

import com.appdist.api.dto.*
import com.appdist.domain.repository.UserRepository
import com.appdist.plugins.AuthPrincipal
import com.appdist.plugins.JWT_AUTH
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.userRoutes(userRepository: UserRepository) {
    authenticate(JWT_AUTH) {
        get("/users/me") {
            val principal = call.principal<AuthPrincipal>()!!
            val user = userRepository.findById(UUID.fromString(principal.userId))
                ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", "User not found"))
            call.respond(UserDto(user.id.toString(), user.email, user.name,
                user.role.name, user.workspaceId.toString()))
        }

        patch("/users/me") {
            val principal = call.principal<AuthPrincipal>()!!
            val req = call.receive<UpdateProfileRequest>()
            val userId = UUID.fromString(principal.userId)
            if (req.fcmToken != null) userRepository.updateFcmToken(userId, req.fcmToken)
            if (req.name != null) userRepository.update(userId, req.name)
            val user = userRepository.findById(userId)!!
            call.respond(UserDto(user.id.toString(), user.email, user.name,
                user.role.name, user.workspaceId.toString()))
        }
    }
}
```

- [ ] **Step 5: Run all tests**

```bash
cd backend && ./gradlew test 2>&1 | tail -10
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add backend/src/
git commit -m "feat: add project and user routes with JWT-protected endpoints"
```

---

## Task 12: Dockerfile + README + Seed Data

**Files:**
- Create: `backend/Dockerfile`
- Create: `backend/README.md`
- Create: `backend/scripts/seed.sh`

- [ ] **Step 1: Dockerfile**

```dockerfile
FROM gradle:8.5-jdk21 AS builder
WORKDIR /app
COPY . .
RUN gradle shadowJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*-all.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 2: README.md**

````markdown
# AppDistribution Backend

Ktor-based backend for distributing test Android APKs.

## Requirements
- JDK 21+
- Docker + Docker Compose

## Quick Start

```bash
# 1. Start infrastructure
docker-compose up -d

# 2. Run backend
./gradlew run

# 3. Backend available at http://localhost:8080
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| DATABASE_URL | jdbc:postgresql://localhost:5432/appdist | PostgreSQL URL |
| DATABASE_USER | appdist | DB user |
| DATABASE_PASSWORD | appdist_secret | DB password |
| MINIO_ENDPOINT | http://localhost:9000 | MinIO endpoint |
| MINIO_ACCESS_KEY | minioadmin | MinIO access key |
| MINIO_SECRET_KEY | minioadmin123 | MinIO secret key |
| JWT_SECRET | change-me-... | JWT signing secret |

## API Examples

### Register + Auth
```bash
# Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@example.com","name":"Admin","workspaceName":"My Team"}'

# Request OTP (check server logs for code)
curl -X POST http://localhost:8080/api/v1/auth/request-otp \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@example.com"}'

# Verify OTP → get tokens
curl -X POST http://localhost:8080/api/v1/auth/verify-otp \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@example.com","otp":"123456"}'
```

### Upload APK
```bash
curl -X POST http://localhost:8080/api/v1/builds/upload \
  -H 'Authorization: Bearer YOUR_TOKEN' \
  -F 'apk=@/path/to/app.apk' \
  -F 'projectId=YOUR_PROJECT_ID' \
  -F 'environment=QA' \
  -F 'channel=INTERNAL' \
  -F 'buildType=debug' \
  -F 'changelog=Bug fixes and improvements'
```

### Get Download URL
```bash
curl http://localhost:8080/api/v1/builds/BUILD_ID/download-url \
  -H 'Authorization: Bearer YOUR_TOKEN'
```
````

- [ ] **Step 3: Final full test run**

```bash
cd backend && ./gradlew test 2>&1 | grep -E "tests|PASS|FAIL|BUILD"
```

Expected: BUILD SUCCESSFUL, all tests passing.

- [ ] **Step 4: Commit**

```bash
git add backend/
git commit -m "chore: add Dockerfile, README with API examples and env vars docs"
```

---

## Компромиссы и известные ограничения

1. **Email отправка** — OTP только в console log, нужно подключить SMTP/SendGrid
2. **Авторизация доступа к builds** — нет проверки принадлежности build к workspace пользователя (добавить в Phase 2)
3. **FCM notifications** — сервис-заглушка, нужен Firebase Admin SDK
4. **Build number** — не извлекается из APK автоматически (нет стандартного поля)
5. **ABIs из apk-parser** — зависит от библиотеки, может отсутствовать для некоторых APK
6. **Manual DI в Routing.kt** — замените на Koin или Dagger когда проект вырастет
