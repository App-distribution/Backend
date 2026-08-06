package com.appdist.api

import com.appdist.TestDatabase
import com.appdist.domain.model.UserRole
import com.appdist.domain.service.ApiKeyService
import com.appdist.infrastructure.database.repository.ApiKeyRepositoryImpl
import com.appdist.infrastructure.database.repository.UserRepositoryImpl
import com.appdist.infrastructure.database.repository.WorkspaceRepositoryImpl
import kotlinx.coroutines.runBlocking
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApiKeyServiceTest {
    private val users = UserRepositoryImpl()
    private val workspaces = WorkspaceRepositoryImpl()
    private val service = ApiKeyService(ApiKeyRepositoryImpl(), users)

    @BeforeTest
    fun setup() {
        TestDatabase.init()
    }

    private fun seed(): Triple<UUID, UUID, String> = runBlocking {
        val ws = workspaces.create("WS ${UUID.randomUUID()}", "slug-${UUID.randomUUID()}")
        val email = "user-${UUID.randomUUID()}@example.com"
        val user = users.create(ws.id, email, "User", UserRole.ADMIN)
        Triple(ws.id, user.id, email)
    }

    @Test
    fun `issued key has expected format`() = runBlocking {
        val (wsId, userId, _) = seed()
        val issued = service.issue(wsId, userId, "CI")

        assertTrue(issued.plainKey.startsWith("appdist_"))
        assertEquals(8 + 32, issued.plainKey.length)
        assertTrue(issued.plainKey.drop(8).all { it.isLetterOrDigit() })
        // Точка сломала бы разделение с JWT-провайдером.
        assertTrue(!issued.plainKey.contains('.'))
    }

    @Test
    fun `plain key is not stored`() = runBlocking {
        val (wsId, userId, _) = seed()
        val issued = service.issue(wsId, userId, "CI")

        assertNotEquals(issued.plainKey, issued.apiKey.keyHash)
        assertEquals(ApiKeyService.hash(issued.plainKey), issued.apiKey.keyHash)
        assertEquals(64, issued.apiKey.keyHash.length)
    }

    @Test
    fun `two keys differ`() = runBlocking {
        val (wsId, userId, _) = seed()
        val first = service.issue(wsId, userId, "one")
        val second = service.issue(wsId, userId, "two")

        assertNotEquals(first.plainKey, second.plainKey)
        assertNotEquals(first.apiKey.keyHash, second.apiKey.keyHash)
    }

    @Test
    fun `authenticate returns identity of the creator`() = runBlocking {
        val (wsId, userId, email) = seed()
        val issued = service.issue(wsId, userId, "CI")

        val identity = service.authenticate(issued.plainKey)
        assertNotNull(identity)
        assertEquals(userId, identity.userId)
        assertEquals(email, identity.email)
        assertEquals(wsId, identity.workspaceId)
    }

    @Test
    fun `authenticate rejects unknown and malformed keys`() = runBlocking {
        assertNull(service.authenticate("appdist_0000000000000000000000000000000"))
        assertNull(service.authenticate("no-prefix-key"))
        assertNull(service.authenticate(""))
    }

    @Test
    fun `revoked key stops authenticating`() = runBlocking {
        val (wsId, userId, _) = seed()
        val issued = service.issue(wsId, userId, "CI")

        assertTrue(service.revoke(issued.apiKey.id, wsId, userId))
        assertNull(service.authenticate(issued.plainKey))
    }

    @Test
    fun `revoke twice returns false`() = runBlocking {
        val (wsId, userId, _) = seed()
        val issued = service.issue(wsId, userId, "CI")

        assertTrue(service.revoke(issued.apiKey.id, wsId, userId))
        assertTrue(!service.revoke(issued.apiKey.id, wsId, userId))
    }

    @Test
    fun `list returns issued keys`() = runBlocking {
        val (wsId, userId, _) = seed()
        service.issue(wsId, userId, "first")
        service.issue(wsId, userId, "second")

        assertEquals(2, service.list(wsId).size)
    }
}
