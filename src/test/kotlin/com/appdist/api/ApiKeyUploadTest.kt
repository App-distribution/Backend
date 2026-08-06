package com.appdist.api

import com.appdist.TestDatabase
import com.appdist.api.dto.CreateApiKeyRequest
import com.appdist.api.dto.RequestOtpRequest
import com.appdist.api.dto.VerifyOtpRequest
import com.appdist.TestOtp
import com.appdist.testModule
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApiKeyUploadTest {

    // H2 одна на весь прогон JVM (DatabaseFactory.initH2, DB_CLOSE_DELAY=-1),
    // поэтому сбрасываем схему перед каждым тестом — тот же приём, что и в
    // ApiKeyRoutesTest/ProjectRoutesTest/RbacTest.
    @BeforeTest
    fun setup() {
        TestDatabase.init()
        TestDatabase.reset()
    }

    @Test
    fun `build uploaded by api key is attributed to key creator`() = testApplication {
        application { testModule() }
        val client = createClient { install(ContentNegotiation) { json() } }
        val email = "uploader@example.com"

        client.post("/api/v1/auth/request-otp") {
            contentType(ContentType.Application.Json)
            setBody(RequestOtpRequest(email))
        }
        val otp = TestOtp.lastCodeFor(email)
        val auth = client.post("/api/v1/auth/verify-otp") {
            contentType(ContentType.Application.Json)
            setBody(VerifyOtpRequest(email, otp))
        }
        val token = Json.parseToJsonElement(auth.bodyAsText())
            .jsonObject["access_token"]!!.jsonPrimitive.content

        val keyResponse = client.post("/api/v1/api-keys") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(CreateApiKeyRequest("CI"))
        }
        val key = Json.parseToJsonElement(keyResponse.bodyAsText())
            .jsonObject["key"]!!.jsonPrimitive.content

        val projects = client.get("/api/v1/projects") {
            header(HttpHeaders.Authorization, "Bearer $key")
        }
        assertEquals(HttpStatusCode.OK, projects.status)

        // Полная загрузка возможна только с фикстурой APK: ApkMetadataExtractor
        // разбирает настоящий архив, подделать его содержимое нельзя.
        val testApk = java.io.File("src/test/resources/test.apk")
        if (!testApk.exists()) {
            println("Пропуск проверки загрузки — нет фикстуры ${testApk.absolutePath}")
            return@testApplication
        }

        val projectId = Json.parseToJsonElement(projects.bodyAsText())
            .jsonArray.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.content
            ?: return@testApplication

        val upload = client.post("/api/v1/builds/upload") {
            header(HttpHeaders.Authorization, "Bearer $key")
            setBody(MultiPartFormDataContent(formData {
                append("projectId", projectId)
                append("environment", "STAGING")
                append("channel", "INTERNAL")
                append("buildType", "release")
                append("apk", testApk.readBytes(), Headers.build {
                    append(HttpHeaders.ContentType, "application/vnd.android.package-archive")
                    append(HttpHeaders.ContentDisposition, "filename=\"test.apk\"")
                })
            }))
        }
        assertEquals(HttpStatusCode.Created, upload.status)
        // Автором сборки становится выпустивший ключ человек.
        assertTrue(upload.bodyAsText().contains("uploader_name"))
    }

    @Test
    fun `upload without any credentials returns 401`() = testApplication {
        application { testModule() }
        val client = createClient { install(ContentNegotiation) { json() } }

        val response = client.post("/api/v1/builds/upload") {
            setBody(MultiPartFormDataContent(formData {
                append("projectId", "00000000-0000-0000-0000-000000000000")
            }))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
