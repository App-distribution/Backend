package com.appdist.api

import com.appdist.TestDatabase
import com.appdist.api.dto.CreateApiKeyRequest
import com.appdist.api.dto.RequestOtpRequest
import com.appdist.api.dto.VerifyOtpRequest
import com.appdist.testModule
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApiKeyRoutesTest {

    // H2 в тестах — один именованный in-memory инстанс на весь JVM-прогон
    // (DatabaseFactory.initH2, DB_CLOSE_DELAY=-1), поэтому без сброса между
    // тестами домен example.com "унаследовал" бы ADMIN только у первого
    // залогинившегося, а остальные admin-*@example.com получили бы TESTER.
    // Тот же приём — в ProjectRoutesTest и RbacTest.
    @BeforeTest
    fun setup() {
        TestDatabase.init()
        TestDatabase.reset()
    }

    // Вход по OTP: код печатается сервисом и одновременно возвращается из
    // requestOtp, но через HTTP его не видно — в тестах логинимся тем же
    // путём, что и остальные route-тесты.
    private suspend fun login(client: io.ktor.client.HttpClient, email: String): String {
        client.post("/api/v1/auth/request-otp") {
            contentType(ContentType.Application.Json)
            setBody(RequestOtpRequest(email))
        }
        val otp = com.appdist.TestOtp.lastCodeFor(email)
        val response = client.post("/api/v1/auth/verify-otp") {
            contentType(ContentType.Application.Json)
            setBody(VerifyOtpRequest(email, otp))
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        return body["access_token"]!!.jsonPrimitive.content
    }

    @Test
    fun `issue returns key exactly once`() = testApplication {
        application { testModule() }
        val client = createClient { install(ContentNegotiation) { json() } }
        val token = login(client, "admin-issue@example.com")

        val created = client.post("/api/v1/api-keys") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(CreateApiKeyRequest("CI stage"))
        }
        assertEquals(HttpStatusCode.Created, created.status)
        val createdBody = Json.parseToJsonElement(created.bodyAsText()).jsonObject
        val plainKey = createdBody["key"]!!.jsonPrimitive.content
        assertTrue(plainKey.startsWith("appdist_"))
        assertEquals("appdist_", createdBody["prefix"]!!.jsonPrimitive.content.take(8))

        val list = client.get("/api/v1/api-keys") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, list.status)
        val listText = list.bodyAsText()
        assertTrue(!listText.contains(plainKey), "полный ключ не должен возвращаться в списке")
        assertTrue(listText.contains("last_used_at"), "поля должны быть в snake_case")
    }

    @Test
    fun `issue rejects empty name`() = testApplication {
        application { testModule() }
        val client = createClient { install(ContentNegotiation) { json() } }
        val token = login(client, "admin-empty@example.com")

        val response = client.post("/api/v1/api-keys") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(CreateApiKeyRequest("   "))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `issue without token returns 401`() = testApplication {
        application { testModule() }
        val client = createClient { install(ContentNegotiation) { json() } }

        val response = client.post("/api/v1/api-keys") {
            contentType(ContentType.Application.Json)
            setBody(CreateApiKeyRequest("CI"))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `revoke returns 204 then 404`() = testApplication {
        application { testModule() }
        val client = createClient { install(ContentNegotiation) { json() } }
        val token = login(client, "admin-revoke@example.com")

        val created = client.post("/api/v1/api-keys") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(CreateApiKeyRequest("to-revoke"))
        }
        val id = Json.parseToJsonElement(created.bodyAsText()).jsonObject["id"]!!.jsonPrimitive.content

        val first = client.delete("/api/v1/api-keys/$id") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.NoContent, first.status)

        val second = client.delete("/api/v1/api-keys/$id") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.NotFound, second.status)
    }

    @Test
    fun `revoke with malformed id returns 400`() = testApplication {
        application { testModule() }
        val client = createClient { install(ContentNegotiation) { json() } }
        val token = login(client, "admin-badid@example.com")

        val response = client.delete("/api/v1/api-keys/not-a-uuid") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `non-admin cannot issue a key`() = testApplication {
        application { testModule() }
        val client = createClient { install(ContentNegotiation) { json() } }

        // Первый пользователь домена получает ADMIN, второй — TESTER:
        // так решает AuthService.verifyOtp при создании рабочего пространства.
        login(client, "first@rbac-keys.example.com")
        val testerToken = login(client, "second@rbac-keys.example.com")

        val response = client.post("/api/v1/api-keys") {
            header(HttpHeaders.Authorization, "Bearer $testerToken")
            contentType(ContentType.Application.Json)
            setBody(CreateApiKeyRequest("from tester"))
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
    }
}
