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
data class WorkspaceDto(val id: String, val name: String, val slug: String)

fun Route.workspaceRoutes(workspaceRepository: WorkspaceRepository) {
    authenticate(JWT_AUTH) {
        get("/workspaces") {
            val principal = call.principal<AuthPrincipal>()!!
            val workspaceId = UUID.fromString(principal.workspaceId)
            val workspace = workspaceRepository.findById(workspaceId)
                ?: return@get call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse("WORKSPACE_NOT_FOUND", "Workspace from token no longer exists")
                )
            call.respond(WorkspaceDto(workspace.id.toString(), workspace.name, workspace.slug))
        }
    }
}
