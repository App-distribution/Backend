package com.appdist.api.routes

import com.appdist.api.dto.ErrorResponse
import com.appdist.domain.repository.WorkspaceRepository
import com.appdist.plugins.AuthPrincipal
import com.appdist.plugins.JWT_AUTH
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class WorkspaceDto(
    val id: String,
    val name: String,
    val slug: String,
    val ownerId: String?,
    val createdAt: String,
)

fun Route.workspaceRoutes(workspaceRepository: WorkspaceRepository) {
    authenticate(JWT_AUTH) {
        get("/workspaces/me") {
            val principal = call.principal<AuthPrincipal>()!!
            val workspaceId = try {
                UUID.fromString(principal.workspaceId)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_ID", "Invalid workspace UUID in token"))
                return@get
            }
            val workspace = workspaceRepository.findById(workspaceId)
                ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse("NOT_FOUND", "Workspace not found")
                )
            call.respond(
                WorkspaceDto(
                    id = workspace.id.toString(),
                    name = workspace.name,
                    slug = workspace.slug,
                    ownerId = workspace.ownerId?.toString(),
                    createdAt = workspace.createdAt.toString(),
                )
            )
        }
    }
}
