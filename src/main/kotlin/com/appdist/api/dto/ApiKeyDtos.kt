package com.appdist.api.dto

import com.appdist.domain.model.ApiKey
import kotlinx.serialization.Serializable

@Serializable
data class CreateApiKeyRequest(val name: String)

/** Ответ на выпуск: единственное место, где отдаётся сам ключ. */
@Serializable
data class IssuedApiKeyDto(
    val id: String,
    val name: String,
    val prefix: String,
    val key: String,
)

@Serializable
data class ApiKeyDto(
    val id: String,
    val name: String,
    val prefix: String,
    val createdAt: Long,
    val lastUsedAt: Long?,
    val revoked: Boolean,
)

fun ApiKey.toDto() = ApiKeyDto(
    id = id.toString(),
    name = name,
    prefix = prefix,
    createdAt = createdAt.toEpochMilliseconds(),
    lastUsedAt = lastUsedAt?.toEpochMilliseconds(),
    revoked = revoked,
)
