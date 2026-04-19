package com.appdist.integration

import com.appdist.api.dto.*
import com.appdist.api.routes.WorkspaceDto
import com.appdist.domain.service.AuthService
import com.appdist.infrastructure.database.repository.*
import io.ktor.client.HttpClient
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class RbacIntegrationTest : IntegrationTestBase() {

    private suspend fun loginAndGetToken(email: String, password: String, client: HttpClient): String {
        val response = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, password))
        }
        return response.body<AuthResponse>().accessToken
    }

    @Test
    fun `Tester cannot create project - returns 403`() = testApplication {
        application { integrationTestModule() }
        val client = createClient { install(ContentNegotiation) { json() } }

        // Bootstrap the admin first, then create a tester user via admin
        val adminToken = loginAndGetToken(TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD, client)

        val wsResp = client.get("/api/v1/workspaces/me") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
        }
        val workspaceId = wsResp.body<WorkspaceDto>().id

        // Create a tester user
        val createResp = client.post("/api/v1/workspaces/$workspaceId/users") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
            contentType(ContentType.Application.Json)
            setBody(CreateUserRequest("tester@integration-test.com", "Tester User", "TESTER"))
        }
        assertEquals(HttpStatusCode.Created, createResp.status)
        val testerPassword = createResp.body<CreateUserResponse>().generatedPassword

        val testerToken = loginAndGetToken("tester@integration-test.com", testerPassword, client)

        val response = client.post("/api/v1/workspaces/$workspaceId/projects") {
            header(HttpHeaders.Authorization, "Bearer $testerToken")
            contentType(ContentType.Application.Json)
            setBody(CreateProjectRequest("MyApp", "com.myapp"))
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `Unauthenticated request returns 401`() = testApplication {
        application { integrationTestModule() }
        val client = createClient { install(ContentNegotiation) { json() } }
        val response = client.get("/api/v1/projects")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `Admin can access users me`() = testApplication {
        application { integrationTestModule() }
        val client = createClient { install(ContentNegotiation) { json() } }

        val adminToken = loginAndGetToken(TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD, client)

        val meResponse = client.get("/api/v1/users/me") {
            header(HttpHeaders.Authorization, "Bearer $adminToken")
        }
        assertEquals(HttpStatusCode.OK, meResponse.status)
    }
}
