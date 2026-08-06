package com.appdist.api.routes

import com.appdist.api.dto.*
import com.appdist.domain.model.UserRole
import com.appdist.domain.service.ApiKeyService
import com.appdist.plugins.AuthPrincipal
import com.appdist.plugins.JWT_AUTH
import com.appdist.plugins.requireRole
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.apiKeyRoutes(apiKeyService: ApiKeyService) {
    // Ключами управляет человек, вошедший по коду: под API_KEY_AUTH эти
    // роуты не подключены намеренно, иначе ключ мог бы выпустить себе смену.
    authenticate(JWT_AUTH) {
        route("/api-keys") {
            post {
                call.requireRole(UserRole.ADMIN)
                val principal = call.principal<AuthPrincipal>()!!
                val req = call.receive<CreateApiKeyRequest>()
                if (req.name.isBlank()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("INVALID_FIELD", "name must not be blank")
                    )
                    return@post
                }
                val issued = apiKeyService.issue(
                    workspaceId = UUID.fromString(principal.workspaceId),
                    createdBy = UUID.fromString(principal.userId),
                    name = req.name.trim(),
                )
                call.respond(
                    HttpStatusCode.Created,
                    IssuedApiKeyDto(
                        id = issued.apiKey.id.toString(),
                        name = issued.apiKey.name,
                        prefix = issued.apiKey.prefix,
                        key = issued.plainKey,
                    )
                )
            }

            get {
                call.requireRole(UserRole.ADMIN)
                val principal = call.principal<AuthPrincipal>()!!
                val keys = apiKeyService.list(UUID.fromString(principal.workspaceId))
                call.respond(keys.map { it.toDto() })
            }

            delete("/{id}") {
                call.requireRole(UserRole.ADMIN)
                val principal = call.principal<AuthPrincipal>()!!
                val id = try {
                    UUID.fromString(call.parameters["id"]!!)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_ID", "Invalid UUID format"))
                    return@delete
                }
                val revoked = apiKeyService.revoke(
                    id = id,
                    workspaceId = UUID.fromString(principal.workspaceId),
                    actorId = UUID.fromString(principal.userId),
                )
                if (revoked) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", "API key not found"))
                }
            }
        }
    }
}
