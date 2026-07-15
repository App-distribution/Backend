package com.appdist.api

import com.appdist.TestDatabase
import com.appdist.api.routes.publicRoutes
import com.appdist.config.AppConfig
import com.appdist.domain.service.BuildService
import com.appdist.infrastructure.database.repository.AuditRepositoryImpl
import com.appdist.infrastructure.database.repository.BuildRepositoryImpl
import com.appdist.infrastructure.database.repository.ProjectRepositoryImpl
import com.appdist.infrastructure.storage.StorageClient
import com.appdist.plugins.configureSerialization
import com.appdist.plugins.configureStatusPages
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import java.util.UUID

class PublicRoutesTest {
    @BeforeTest
    fun resetDb() {
        TestDatabase.init()
        TestDatabase.reset()
    }

    @Test
    fun `public project list does not require authentication`() = publicTestApplication {
        val response = client.get("/api/v1/public/projects")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `public download returns not found without authentication`() = publicTestApplication {
        val response = client.get("/api/v1/public/builds/${UUID.randomUUID()}/download-url")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `public build detail returns not found without authentication`() = publicTestApplication {
        val response = client.get("/api/v1/public/builds/${UUID.randomUUID()}")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    private fun publicTestApplication(block: suspend io.ktor.server.testing.ApplicationTestBuilder.() -> Unit) =
        testApplication {
            application {
                configureSerialization()
                configureStatusPages()
                val buildService = BuildService(
                    buildRepository = BuildRepositoryImpl(),
                    storageClient = mockk<StorageClient>(relaxed = true),
                    auditRepository = AuditRepositoryImpl(),
                    storageConfig = AppConfig.StorageConfig(
                        endpoint = "http://localhost:9000",
                        publicEndpoint = "http://localhost:9000",
                        accessKey = "test",
                        secretKey = "test",
                        bucket = "test",
                    ),
                )
                routing {
                    route("/api/v1") {
                        publicRoutes(ProjectRepositoryImpl(), buildService)
                    }
                }
            }
            block()
        }
}
