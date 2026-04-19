package com.appdist.api.routes

import com.appdist.api.dto.*
import com.appdist.domain.model.UserRole
import com.appdist.domain.repository.UserRepository
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

fun Route.userRoutes(userRepository: UserRepository) {
    authenticate(JWT_AUTH) {
        get("/users/me") {
            val principal = call.principal<AuthPrincipal>()!!
            val user = userRepository.findById(UUID.fromString(principal.userId))
                ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", "User not found"))
            call.respond(
                UserDto(
                    id = user.id.toString(),
                    email = user.email,
                    name = user.name,
                    role = user.role.name,
                    workspaceId = user.workspaceId?.toString(),
                    createdAt = user.createdAt.toString(),
                )
            )
        }

        patch("/users/me") {
            val principal = call.principal<AuthPrincipal>()!!
            val req = call.receive<UpdateProfileRequest>()

            if (req.name == null && req.fcmToken == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("NO_FIELDS_TO_UPDATE", "At least one field must be provided"))
                return@patch
            }

            val userId = UUID.fromString(principal.userId)
            if (req.fcmToken != null) userRepository.updateFcmToken(userId, req.fcmToken)
            if (req.name != null) userRepository.update(userId, req.name)
            val user = userRepository.findById(userId)!!
            call.respond(
                UserDto(
                    id = user.id.toString(),
                    email = user.email,
                    name = user.name,
                    role = user.role.name,
                    workspaceId = user.workspaceId?.toString(),
                    createdAt = user.createdAt.toString(),
                )
            )
        }

        get("/workspaces/{workspaceId}/users") {
            call.requireRole(UserRole.ADMIN)
            val workspaceId = try {
                UUID.fromString(call.parameters["workspaceId"]!!)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_ID", "Invalid UUID format"))
                return@get
            }
            val users = userRepository.findAllByWorkspace(workspaceId)
            call.respond(users.map { user ->
                UserDto(
                    id = user.id.toString(),
                    email = user.email,
                    name = user.name,
                    role = user.role.name,
                    workspaceId = user.workspaceId?.toString(),
                    createdAt = user.createdAt.toString(),
                )
            })
        }
    }
}
