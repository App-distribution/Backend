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
import kotlin.test.assertTrue

class AuthRoutesTest {
    @Test
    fun `login returns 200 and tokens for valid credentials`() = testApplication {
        application { testModule() }
        val client = createClient { install(ContentNegotiation) { json() } }
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
