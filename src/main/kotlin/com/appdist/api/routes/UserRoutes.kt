package com.appdist.api.routes

import com.appdist.api.dto.*
import com.appdist.domain.repository.UserRepository
import com.appdist.plugins.AuthPrincipal
import com.appdist.plugins.JWT_AUTH
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
                    user.id.toString(),
                    user.email,
                    user.name,
                    user.role.name,
                    user.workspaceId?.toString()
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
                    user.id.toString(),
                    user.email,
                    user.name,
                    user.role.name,
                    user.workspaceId?.toString()
                )
            )
        }
    }
}
