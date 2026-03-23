package com.appdist.integration

import com.appdist.api.dto.*
import com.appdist.api.routes.WorkspaceDto
import io.ktor.client.HttpClient
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class RbacIntegrationTest : IntegrationTestBase() {

    private suspend fun loginAndGetToken(email: String, client: HttpClient): String {
        client.post("/api/v1/auth/request-otp") { contentType(ContentType.Application.Json); setBody(RequestOtpRequest(email)) }
        val otp = captureLastOtp(email)
        val response = client.post("/api/v1/auth/verify-otp") { contentType(ContentType.Application.Json); setBody(VerifyOtpRequest(email, otp)) }
        return response.body<AuthResponse>().accessToken
    }

    @Test
    fun `Tester cannot create project - returns 403`() = testApplication {
        application { integrationTestModule() }
        val client = createClient { install(ContentNegotiation) { json() } }

        val domain = "rbac-${System.currentTimeMillis()}.com"
        loginAndGetToken("admin@$domain", client)   // first user = ADMIN, creates workspace
        val testerToken = loginAndGetToken("tester@$domain", client)  // second = TESTER

        val wsResp = client.get("/api/v1/workspaces/me") { header(HttpHeaders.Authorization, "Bearer $testerToken") }
        val workspaceId = wsResp.body<WorkspaceDto>().id

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

        val domain = "admin-${System.currentTimeMillis()}.com"
        val adminToken = loginAndGetToken("admin@$domain", client)

        val meResponse = client.get("/api/v1/users/me") { header(HttpHeaders.Authorization, "Bearer $adminToken") }
        assertEquals(HttpStatusCode.OK, meResponse.status)
    }
}
