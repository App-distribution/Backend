package com.appdist.integration

import com.appdist.api.dto.AuthResponse
import com.appdist.api.dto.LoginRequest
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
    fun `full auth flow - bootstrap admin can login and receives tokens`() = testApplication {
        application { integrationTestModule() }
        val client = createClient { install(ContentNegotiation) { json() } }

        val loginResponse = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(TEST_ADMIN_EMAIL, TEST_ADMIN_PASSWORD))
        }
        assertEquals(HttpStatusCode.OK, loginResponse.status)
        val tokens = loginResponse.body<AuthResponse>()
        assertNotNull(tokens.accessToken)
        assertNotNull(tokens.refreshToken)

        transaction {
            val user = UsersTable.selectAll().where { UsersTable.email eq TEST_ADMIN_EMAIL }.single()
            assertEquals("ADMIN", user[UsersTable.role])
        }
    }

    @Test
    fun `login with wrong password returns 401`() = testApplication {
        application { integrationTestModule() }
        val client = createClient { install(ContentNegotiation) { json() } }

        val loginResponse = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(TEST_ADMIN_EMAIL, "wrongpassword"))
        }
        assertEquals(HttpStatusCode.Unauthorized, loginResponse.status)
    }
}
