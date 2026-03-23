package com.appdist.api

import com.appdist.TestDatabase
import com.appdist.api.dto.*
import com.appdist.testModule
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RbacTest {
    @BeforeTest
    fun setup() {
        TestDatabase.init()
        TestDatabase.reset()
    }

    @Test
    fun `POST workspaces-id-projects without auth returns 401`() = testApplication {
        application { testModule() }
        val client = createClient { install(ContentNegotiation) { json() } }
        val response = client.post("/api/v1/workspaces/00000000-0000-0000-0000-000000000001/projects") {
            contentType(ContentType.Application.Json)
            setBody(CreateProjectRequest("test", "com.test"))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `DELETE projects-id without auth returns 401`() = testApplication {
        application { testModule() }
        val client = createClient { install(ContentNegotiation) { json() } }
        val response = client.delete("/api/v1/projects/00000000-0000-0000-0000-000000000001")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET users-me with malformed bearer token returns 401`() = testApplication {
        application { testModule() }
        val client = createClient { install(ContentNegotiation) { json() } }
        val response = client.get("/api/v1/users/me") {
            header(HttpHeaders.Authorization, "Bearer undefined")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
