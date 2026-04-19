package com.appdist.api.routes

import com.appdist.api.dto.*
import com.appdist.domain.model.UserRole
import com.appdist.domain.repository.UserRepository
import com.appdist.domain.service.AuthService
import com.appdist.plugins.AuthPrincipal
import com.appdist.plugins.JWT_AUTH
import com.appdist.plugins.requireRole
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.exceptions.ExposedSQLException
import java.util.UUID

fun Route.userManagementRoutes(authService: AuthService, userRepository: UserRepository) {
    authenticate(JWT_AUTH) {
        route("/workspaces/{workspaceId}/users") {
            post {
                call.requireRole(UserRole.ADMIN)
                val workspaceId = runCatching {
                    UUID.fromString(call.parameters["workspaceId"])
                }.getOrElse {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_ID", "Invalid workspace UUID"))
                    return@post
                }
                val req = runCatching { call.receive<CreateUserRequest>() }.getOrElse {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_REQUEST", "Missing required fields"))
                    return@post
                }
                if (req.email.isBlank() || req.name.isBlank() || req.role.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_REQUEST", "email, name, role required"))
                    return@post
                }
                val role = runCatching { UserRole.valueOf(req.role.uppercase()) }.getOrElse {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_REQUEST", "Invalid role"))
                    return@post
                }
                val (user, plainPassword) = runCatching {
                    authService.createUser(workspaceId, req.email.trim(), req.name.trim(), role)
                }.getOrElse { e ->
                    if (e is ExposedSQLException && e.message?.contains("unique", ignoreCase = true) == true) {
                        call.respond(HttpStatusCode.Conflict, ErrorResponse("USER_ALREADY_EXISTS", "Email already registered"))
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, ErrorResponse("SERVER_ERROR", "Failed to create user"))
                    }
                    return@post
                }
                call.respond(
                    HttpStatusCode.Created,
                    CreateUserResponse(
                        user = UserDto(
                            id = user.id.toString(),
                            email = user.email,
                            name = user.name,
                            role = user.role.name,
                            workspaceId = user.workspaceId?.toString(),
                            createdAt = user.createdAt.toString(),
                        ),
                        generatedPassword = plainPassword,
                    )
                )
            }
        }

        post("/workspaces/{workspaceId}/users/{userId}/reset-password") {
            call.requireRole(UserRole.ADMIN)
            val userId = runCatching {
                UUID.fromString(call.parameters["userId"])
            }.getOrElse {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_ID", "Invalid user UUID"))
                return@post
            }
            val user = userRepository.findById(userId)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("USER_NOT_FOUND", "User not found"))
                return@post
            }
            val newPassword = authService.resetPassword(userId)
            call.respond(HttpStatusCode.OK, ResetPasswordResponse(generatedPassword = newPassword))
        }
    }
}
