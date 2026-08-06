package com.appdist.api

import com.appdist.api.dto.CreateApiKeyRequest
import com.appdist.api.dto.RequestOtpRequest
import com.appdist.api.dto.VerifyOtpRequest
import com.appdist.TestDatabase
import com.appdist.TestOtp
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

class ApiKeyAuthTest {

    @BeforeTest
    fun resetDb() {
        TestDatabase.init()
        TestDatabase.reset()
    }

    private suspend fun issueKey(client: io.ktor.client.HttpClient, email: String): String {
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

        val created = client.post("/api/v1/api-keys") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(CreateApiKeyRequest("CI"))
        }
        return Json.parseToJsonElement(created.bodyAsText())
            .jsonObject["key"]!!.jsonPrimitive.content
    }

    @Test
    fun `api key opens projects listing`() = testApplication {
        application { testModule() }
        val client = createClient { install(ContentNegotiation) { json() } }
        val key = issueKey(client, "admin-projects@example.com")

        val response = client.get("/api/v1/projects") {
            header(HttpHeaders.Authorization, "Bearer $key")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `api key is rejected on routes without the provider`() = testApplication {
        application { testModule() }
        val client = createClient { install(ContentNegotiation) { json() } }
        val key = issueKey(client, "admin-scope@example.com")

        // Управление ключами ключом же — запрещено.
        val response = client.get("/api/v1/api-keys") {
            header(HttpHeaders.Authorization, "Bearer $key")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `unknown api key returns 401`() = testApplication {
        application { testModule() }
        val client = createClient { install(ContentNegotiation) { json() } }

        val response = client.get("/api/v1/projects") {
            header(HttpHeaders.Authorization, "Bearer appdist_unknownkeyunknownkeyunknownkey1")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `revoked api key returns 401`() = testApplication {
        application { testModule() }
        val client = createClient { install(ContentNegotiation) { json() } }
        val email = "admin-revoked@example.com"

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

        val created = client.post("/api/v1/api-keys") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(CreateApiKeyRequest("temp"))
        }
        val body = Json.parseToJsonElement(created.bodyAsText()).jsonObject
        val key = body["key"]!!.jsonPrimitive.content
        val id = body["id"]!!.jsonPrimitive.content

        client.delete("/api/v1/api-keys/$id") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        val response = client.get("/api/v1/projects") {
            header(HttpHeaders.Authorization, "Bearer $key")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `jwt still works on shared routes`() = testApplication {
        application { testModule() }
        val client = createClient { install(ContentNegotiation) { json() } }
        val email = "admin-jwt@example.com"

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

        val response = client.get("/api/v1/projects") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }
}
