package com.appdist.domain.service

import com.appdist.domain.model.ApiKey
import com.appdist.domain.repository.ApiKeyRepository
import com.appdist.domain.repository.AuditRepository
import com.appdist.domain.repository.UserRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

private val log = KotlinLogging.logger {}

class ApiKeyService(
    private val repository: ApiKeyRepository,
    private val userRepository: UserRepository,
    private val auditRepository: AuditRepository? = null,
) : CoroutineScope by CoroutineScope(SupervisorJob() + Dispatchers.IO) {

    data class IssuedKey(val apiKey: ApiKey, val plainKey: String)

    data class ApiKeyIdentity(
        val keyId: UUID,
        val userId: UUID,
        val email: String,
        val workspaceId: UUID,
    )

    private val secureRandom = SecureRandom()

    suspend fun issue(workspaceId: UUID, createdBy: UUID, name: String): IssuedKey {
        val plainKey = generateKey()
        val apiKey = ApiKey(
            id = UUID.randomUUID(),
            workspaceId = workspaceId,
            createdBy = createdBy,
            name = name,
            keyHash = hash(plainKey),
            prefix = plainKey.take(PREFIX_LENGTH),
            createdAt = Clock.System.now(),
            lastUsedAt = null,
            revoked = false,
        )
        val saved = repository.create(apiKey)

        launch {
            runCatching {
                auditRepository?.log(createdBy, "api_key.create", "api_key", saved.id, mapOf("name" to name))
            }.onFailure { log.warn(it) { "Audit log failed for api_key.create" } }
        }

        return IssuedKey(saved, plainKey)
    }

    /**
     * Проверяет ключ и собирает данные для принципала. Пользователь из
     * created_by обязателен: на него ссылается uploader_id сборки, и без него
     * загрузка упала бы на внешнем ключе уже после аутентификации.
     */
    suspend fun authenticate(plainKey: String): ApiKeyIdentity? {
        if (!plainKey.startsWith(PREFIX)) return null
        val apiKey = repository.findActiveByHash(hash(plainKey)) ?: return null
        val user = userRepository.findById(apiKey.createdBy) ?: return null

        launch {
            runCatching { repository.touchLastUsed(apiKey.id) }
                .onFailure { log.warn(it) { "Failed to update last_used_at for ${apiKey.id}" } }
        }

        return ApiKeyIdentity(apiKey.id, apiKey.createdBy, user.email, apiKey.workspaceId)
    }

    suspend fun list(workspaceId: UUID): List<ApiKey> = repository.listByWorkspace(workspaceId)

    suspend fun revoke(id: UUID, workspaceId: UUID, actorId: UUID): Boolean {
        val revoked = repository.revoke(id, workspaceId)
        if (revoked) {
            launch {
                runCatching {
                    auditRepository?.log(actorId, "api_key.revoke", "api_key", id)
                }.onFailure { log.warn(it) { "Audit log failed for api_key.revoke" } }
            }
        }
        return revoked
    }

    private fun generateKey(): String {
        val body = StringBuilder(KEY_BODY_LENGTH)
        repeat(KEY_BODY_LENGTH) {
            body.append(ALPHABET[secureRandom.nextInt(ALPHABET.length)])
        }
        return PREFIX + body
    }

    companion object {
        const val PREFIX = "appdist_"

        // Без точек: JWT-провайдер отбирает токены по их количеству, и точка
        // в ключе сделала бы два провайдера неразличимыми.
        private const val ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        private const val KEY_BODY_LENGTH = 32
        private const val PREFIX_LENGTH = 14

        fun hash(key: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(key.toByteArray())
                .joinToString("") { "%02x".format(it) }
    }
}
