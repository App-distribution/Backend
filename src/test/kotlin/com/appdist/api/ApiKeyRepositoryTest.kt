package com.appdist.api

import com.appdist.TestDatabase
import com.appdist.domain.model.ApiKey
import com.appdist.infrastructure.database.repository.ApiKeyRepositoryImpl
import com.appdist.infrastructure.database.repository.UserRepositoryImpl
import com.appdist.infrastructure.database.repository.WorkspaceRepositoryImpl
import com.appdist.domain.model.UserRole
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApiKeyRepositoryTest {
    private val repo = ApiKeyRepositoryImpl()
    private val users = UserRepositoryImpl()
    private val workspaces = WorkspaceRepositoryImpl()

    @BeforeTest
    fun setup() {
        TestDatabase.init()
    }

    private fun newKey(workspaceId: UUID, userId: UUID, hash: String, name: String = "CI") = ApiKey(
        id = UUID.randomUUID(),
        workspaceId = workspaceId,
        createdBy = userId,
        name = name,
        keyHash = hash,
        prefix = "appdist_abc123",
        createdAt = Clock.System.now(),
        lastUsedAt = null,
        revoked = false,
    )

    private fun seedUser(): Pair<UUID, UUID> = runBlocking {
        val ws = workspaces.create("Test ${UUID.randomUUID()}", "slug-${UUID.randomUUID()}")
        val user = users.create(ws.id, "user-${UUID.randomUUID()}@example.com", "User", UserRole.ADMIN)
        ws.id to user.id
    }

    @Test
    fun `created key is found by hash`() = runBlocking {
        val (wsId, userId) = seedUser()
        val hash = "hash-${UUID.randomUUID()}"
        repo.create(newKey(wsId, userId, hash))

        val found = repo.findActiveByHash(hash)
        assertNotNull(found)
        assertEquals(wsId, found.workspaceId)
        assertEquals(userId, found.createdBy)
        assertFalse(found.revoked)
        assertNull(found.lastUsedAt)
    }

    @Test
    fun `unknown hash is not found`() = runBlocking {
        assertNull(repo.findActiveByHash("no-such-hash"))
    }

    @Test
    fun `revoked key is not returned as active`() = runBlocking {
        val (wsId, userId) = seedUser()
        val hash = "hash-${UUID.randomUUID()}"
        val key = repo.create(newKey(wsId, userId, hash))

        assertTrue(repo.revoke(key.id, wsId))
        assertNull(repo.findActiveByHash(hash))
    }

    @Test
    fun `revoke from another workspace does nothing`(): Unit = runBlocking {
        val (wsId, userId) = seedUser()
        val (otherWsId, _) = seedUser()
        val hash = "hash-${UUID.randomUUID()}"
        val key = repo.create(newKey(wsId, userId, hash))

        assertFalse(repo.revoke(key.id, otherWsId))
        assertNotNull(repo.findActiveByHash(hash))
    }

    @Test
    fun `list returns only keys of the workspace`() = runBlocking {
        val (wsId, userId) = seedUser()
        val (otherWsId, otherUserId) = seedUser()
        repo.create(newKey(wsId, userId, "hash-${UUID.randomUUID()}", "mine"))
        repo.create(newKey(otherWsId, otherUserId, "hash-${UUID.randomUUID()}", "foreign"))

        val list = repo.listByWorkspace(wsId)
        assertEquals(1, list.size)
        assertEquals("mine", list.first().name)
    }

    @Test
    fun `touchLastUsed fills the timestamp`(): Unit = runBlocking {
        val (wsId, userId) = seedUser()
        val hash = "hash-${UUID.randomUUID()}"
        val key = repo.create(newKey(wsId, userId, hash))

        repo.touchLastUsed(key.id)

        assertNotNull(repo.findActiveByHash(hash)?.lastUsedAt)
    }
}
