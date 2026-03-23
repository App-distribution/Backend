package com.appdist.integration

import com.appdist.api.dto.*
import com.appdist.api.routes.WorkspaceDto
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UploadFlowIntegrationTest : IntegrationTestBase() {

    @Test
    fun `upload APK returns 201 with build metadata`() = testApplication {
        application { integrationTestModule() }
        val client = createClient { install(ContentNegotiation) { json() } }

        // Login
        val email = "uploader@upload-test-${System.currentTimeMillis()}.com"
        client.post("/api/v1/auth/request-otp") { contentType(ContentType.Application.Json); setBody(RequestOtpRequest(email)) }
        val otp = captureLastOtp(email)
        val loginResponse = client.post("/api/v1/auth/verify-otp") { contentType(ContentType.Application.Json); setBody(VerifyOtpRequest(email, otp)) }
        val token = loginResponse.body<AuthResponse>().accessToken

        // Get workspace
        val wsResp = client.get("/api/v1/workspaces/me") { header(HttpHeaders.Authorization, "Bearer $token") }
        val workspaceId = wsResp.body<WorkspaceDto>().id

        // Create project
        val projectResp = client.post("/api/v1/workspaces/$workspaceId/projects") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(CreateProjectRequest("TestApp", "com.test.app"))
        }
        assertEquals(HttpStatusCode.Created, projectResp.status)
        val projectId = projectResp.body<ProjectDto>().id

        // Upload APK (skip if test fixture not available)
        val testApk = File("src/test/resources/test.apk")
        if (!testApk.exists()) {
            println("Skipping upload test — test.apk fixture not found at ${testApk.absolutePath}")
            return@testApplication
        }

        val uploadResponse = client.post("/api/v1/builds/upload") {
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(MultiPartFormDataContent(formData {
                append("projectId", projectId)
                append("environment", "QA")
                append("channel", "INTERNAL")
                append("buildType", "debug")
                append("apk", testApk.readBytes(), Headers.build {
                    append(HttpHeaders.ContentType, "application/vnd.android.package-archive")
                    append(HttpHeaders.ContentDisposition, "filename=\"test.apk\"")
                })
            }))
        }
        assertEquals(HttpStatusCode.Created, uploadResponse.status)
        val build = uploadResponse.body<BuildDto>()
        assertNotNull(build.id)
        assertNotNull(build.checksumSha256)
    }
}
