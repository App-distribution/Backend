package com.appdist.integration

import com.appdist.api.dto.AuthResponse
import com.appdist.api.dto.RequestOtpRequest
import com.appdist.api.dto.VerifyOtpRequest
import com.appdist.infrastructure.database.tables.UsersTable
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AuthFlowIntegrationTest : IntegrationTestBase() {

    @Test
    fun `full auth flow - new user gets workspace and Admin role`() = testApplication {
        application { integrationTestModule() }
        val client = createClient { install(ContentNegotiation) { json() } }

        val email = "founder@testcorp-${System.currentTimeMillis()}.com"

        val otpResponse = client.post("/api/v1/auth/request-otp") {
            contentType(ContentType.Application.Json)
            setBody(RequestOtpRequest(email))
        }
        assertEquals(HttpStatusCode.OK, otpResponse.status)

        val capturedOtp = captureLastOtp(email)

        val verifyResponse = client.post("/api/v1/auth/verify-otp") {
            contentType(ContentType.Application.Json)
            setBody(VerifyOtpRequest(email, capturedOtp))
        }
        assertEquals(HttpStatusCode.OK, verifyResponse.status)
        val tokens = verifyResponse.body<AuthResponse>()
        assertNotNull(tokens.accessToken)
        assertNotNull(tokens.refreshToken)

        transaction {
            val user = UsersTable.selectAll().where { UsersTable.email eq email }.single()
            assertEquals("ADMIN", user[UsersTable.role])
        }
    }

    @Test
    fun `second user from same domain gets Tester role`() = testApplication {
        application { integrationTestModule() }
        val client = createClient { install(ContentNegotiation) { json() } }

        val domain = "shared-${System.currentTimeMillis()}.com"
        val email1 = "admin@$domain"
        val email2 = "dev@$domain"

        client.post("/api/v1/auth/request-otp") { contentType(ContentType.Application.Json); setBody(RequestOtpRequest(email1)) }
        client.post("/api/v1/auth/verify-otp") { contentType(ContentType.Application.Json); setBody(VerifyOtpRequest(email1, captureLastOtp(email1))) }

        client.post("/api/v1/auth/request-otp") { contentType(ContentType.Application.Json); setBody(RequestOtpRequest(email2)) }
        val verifyResponse = client.post("/api/v1/auth/verify-otp") {
            contentType(ContentType.Application.Json)
            setBody(VerifyOtpRequest(email2, captureLastOtp(email2)))
        }
        assertEquals(HttpStatusCode.OK, verifyResponse.status)

        transaction {
            val user2 = UsersTable.selectAll().where { UsersTable.email eq email2 }.single()
            assertEquals("TESTER", user2[UsersTable.role])
        }
    }
}
