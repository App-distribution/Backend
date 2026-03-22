package com.appdist.api

import com.appdist.TestDatabase
import com.appdist.config.AppConfig
import com.appdist.domain.model.UserRole
import com.appdist.infrastructure.database.repository.AuditRepositoryImpl
import com.appdist.infrastructure.database.repository.BuildRepositoryImpl
import com.appdist.infrastructure.storage.StorageClient
import com.appdist.domain.service.BuildService
import com.appdist.plugins.configureAuth
import com.appdist.plugins.configureSerialization
import com.appdist.plugins.configureStatusPages
import com.appdist.api.routes.buildRoutes
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import java.io.InputStream
import java.util.Date
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

class BuildRoutesTest {

    private val jwtConfig = AppConfig.JwtConfig(
        secret = "test-secret-key-for-testing-only-32chars-min",
        issuer = "appdist",
        audience = "appdist-client",
        accessTokenTtlMinutes = 60L,
        refreshTokenTtlDays = 30L,
    )

    private val storageConfig = AppConfig.StorageConfig(
        endpoint = "http://localhost:9000",
        accessKey = "minioadmin",
        secretKey = "minioadmin",
        bucket = "test-bucket",
    )

    private val mockStorage: StorageClient = mockk<StorageClient>(relaxed = true).also { mock ->
        every { mock.generateDownloadUrl(any(), any()) } returns "https://example.com/download/test.apk"
    }

    @BeforeTest
    fun resetDb() {
        TestDatabase.init()   // ensures DB is connected before reset
        TestDatabase.reset()
    }

    /** Issue a JWT with the same parameters as [com.appdist.domain.service.AuthService.issueTokens]. */
    private fun issueTestToken(
        userId: UUID = UUID.randomUUID(),
        email: String = "test@appdist.com",
        role: UserRole = UserRole.ADMIN,
        workspaceId: UUID = UUID.randomUUID(),
    ): String = JWT.create()
        .withIssuer(jwtConfig.issuer)
        .withAudience(jwtConfig.audience)
        .withSubject(userId.toString())
        .withClaim("email", email)
        .withClaim("role", role.name)
        .withClaim("workspace_id", workspaceId.toString())
        .withExpiresAt(Date.from((Clock.System.now() + 60.minutes).toJavaInstant()))
        .sign(Algorithm.HMAC256(jwtConfig.secret))

    // ---------------------------------------------------------------------------
    // Helper: build a test application with build routes registered
    // ---------------------------------------------------------------------------
    private fun buildTestApp(block: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            application {
                configureSerialization()
                configureStatusPages()
                configureAuth(jwtConfig)

                val buildService = BuildService(
                    buildRepository = BuildRepositoryImpl(),
                    storageClient = mockStorage,
                    auditRepository = AuditRepositoryImpl(),
                    storageConfig = storageConfig,
                )
                routing {
                    route("/api/v1") {
                        buildRoutes(buildService)
                    }
                }
            }
            block()
        }

    // ---------------------------------------------------------------------------
    // Auth failure tests — no token supplied
    // ---------------------------------------------------------------------------

    @Test
    fun `GET project builds returns 401 without token`() = buildTestApp {
        val client = createClient { install(ContentNegotiation) { json() } }
        val response = client.get("/api/v1/projects/${UUID.randomUUID()}/builds")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET build by id returns 401 without token`() = buildTestApp {
        val client = createClient { install(ContentNegotiation) { json() } }
        val response = client.get("/api/v1/builds/${UUID.randomUUID()}")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `PATCH build returns 401 without token`() = buildTestApp {
        val client = createClient { install(ContentNegotiation) { json() } }
        val response = client.patch("/api/v1/builds/${UUID.randomUUID()}") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `DELETE build returns 401 without token`() = buildTestApp {
        val client = createClient { install(ContentNegotiation) { json() } }
        val response = client.delete("/api/v1/builds/${UUID.randomUUID()}")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET build download-url returns 401 without token`() = buildTestApp {
        val client = createClient { install(ContentNegotiation) { json() } }
        val response = client.get("/api/v1/builds/${UUID.randomUUID()}/download-url")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    // ---------------------------------------------------------------------------
    // Not-found tests — valid JWT but non-existent IDs
    // ---------------------------------------------------------------------------

    @Test
    fun `GET project builds returns 200 with empty list for unknown project`() = buildTestApp {
        val token = issueTestToken()
        val client = createClient { install(ContentNegotiation) { json() } }
        val response = client.get("/api/v1/projects/${UUID.randomUUID()}/builds") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `GET build by id returns 404 when build does not exist`() = buildTestApp {
        val token = issueTestToken()
        val client = createClient { install(ContentNegotiation) { json() } }
        val response = client.get("/api/v1/builds/${UUID.randomUUID()}") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PATCH build returns 404 when build does not exist`() = buildTestApp {
        val token = issueTestToken()
        val client = createClient { install(ContentNegotiation) { json() } }
        val response = client.patch("/api/v1/builds/${UUID.randomUUID()}") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody("""{"changelog":"update notes"}""")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `DELETE build returns 404 when build does not exist`() = buildTestApp {
        val token = issueTestToken()
        val client = createClient { install(ContentNegotiation) { json() } }
        val response = client.delete("/api/v1/builds/${UUID.randomUUID()}") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET build download-url returns 404 when build does not exist`() = buildTestApp {
        val token = issueTestToken()
        val client = createClient { install(ContentNegotiation) { json() } }
        val response = client.get("/api/v1/builds/${UUID.randomUUID()}/download-url") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
