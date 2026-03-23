package com.appdist.api

import com.appdist.TestDatabase
import com.appdist.testModule
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ProjectRoutesTest {
    @BeforeTest
    fun setup() {
        TestDatabase.init()
        TestDatabase.reset()
    }

    @Test
    fun `GET projects without auth returns 401`() = testApplication {
        application { testModule() }
        val client = createClient { install(ContentNegotiation) { json() } }
        val response = client.get("/api/v1/projects")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
