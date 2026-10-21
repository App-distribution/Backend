package com.appdist.api

import com.appdist.TestDatabase
import com.appdist.domain.model.*
import com.appdist.domain.service.normalizeBuildTag
import com.appdist.infrastructure.database.repository.BuildRepositoryImpl
import com.appdist.infrastructure.database.repository.ProjectRepositoryImpl
import com.appdist.infrastructure.database.repository.UserRepositoryImpl
import com.appdist.infrastructure.database.repository.WorkspaceRepositoryImpl
import com.appdist.testModule
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.serialization.json.*
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BuildFiltersTest {
    private val repo = BuildRepositoryImpl()
    private lateinit var projectId: UUID
    private lateinit var userId: UUID

    @BeforeTest
    fun setup() = runBlocking {
        TestDatabase.init()
        TestDatabase.reset()
        val workspace = WorkspaceRepositoryImpl().create("WS", "ws")
        userId = UserRepositoryImpl().create(workspace.id, "ci@example.com", "CI", UserRole.ADMIN).id
        projectId = ProjectRepositoryImpl().create(workspace.id, "App", "com.example.app").id
    }

    private suspend fun build(
        flavor: String?,
        stand: String?,
        status: BuildStatus = BuildStatus.ACTIVE,
    ) = repo.create(
        Build(
            id = UUID.randomUUID(), projectId = projectId, versionName = "1.0", versionCode = 1,
            buildNumber = null, flavor = flavor, buildType = "release",
            environment = BuildEnvironment.QA, channel = ReleaseChannel.INTERNAL,
            branch = null, commitHash = null, uploaderId = userId, uploadDate = Clock.System.now(),
            changelog = null, fileSizeBytes = 1, checksumSha256 = UUID.randomUUID().toString(),
            minSdk = 24, targetSdk = 34, certFingerprint = null, abis = emptyList(),
            storageKey = "k/${UUID.randomUUID()}", status = status, expiryDate = null,
            isLatestInChannel = false, stand = stand,
        )
    )

    @Test
    fun `stand is stored and read back`() = runBlocking {
        val created = build("stage", "l10")
        assertEquals("l10", repo.findById(created.id)!!.stand)
    }

    @Test
    fun `builds can be filtered by flavor and stand together`() = runBlocking {
        build("stage", "l10")
        build("stage", "dev3")
        build("prod", "l10")
        build(null, null)

        val stageOnL10 = repo.listByProject(projectId, flavor = "stage", stand = "l10")
        assertEquals(listOf("stage" to "l10"), stageOnL10.map { it.flavor to it.stand })
        assertEquals(2, repo.listByProject(projectId, stand = "l10").size)
        assertEquals(4, repo.listByProject(projectId).size)
    }

    @Test
    fun `filter values are those used by visible builds, most frequent first`() = runBlocking {
        build("stage", "l10")
        build("stage", "l10")
        build("prod-rustore", "prod")
        build(null, null)
        build("dev", "dev4", status = BuildStatus.ARCHIVED)

        val values = repo.filterValues(projectId)

        assertEquals(listOf("stage", "prod-rustore"), values.flavors)
        assertEquals(listOf("l10", "prod"), values.stands)
    }

    @Test
    fun `tags are trimmed, lowercased and blank means absent`() {
        assertEquals("l10", normalizeBuildTag("  L10 "))
        assertEquals("stage-rustore", normalizeBuildTag("Stage-RuStore"))
        assertNull(normalizeBuildTag("   "))
        assertNull(normalizeBuildTag(null))
    }

    @Test
    fun `public API exposes filter values and filters the build list`() = testApplication {
        application { testModule() }
        build("stage", "l10")
        build("prod", "dev3")

        val filters = client.get("/api/v1/public/projects/$projectId/build-filters")
        assertEquals(HttpStatusCode.OK, filters.status)
        val body = Json.parseToJsonElement(filters.bodyAsText()).jsonObject
        assertEquals(setOf("stage", "prod"), body["flavors"]!!.jsonArray.map { it.jsonPrimitive.content }.toSet())
        assertEquals(setOf("l10", "dev3"), body["stands"]!!.jsonArray.map { it.jsonPrimitive.content }.toSet())

        val list = client.get("/api/v1/public/projects/$projectId/builds?stand=l10")
        val items = Json.parseToJsonElement(list.bodyAsText()).jsonArray
        assertEquals(listOf("l10"), items.map { it.jsonObject["stand"]!!.jsonPrimitive.content })
    }
}
