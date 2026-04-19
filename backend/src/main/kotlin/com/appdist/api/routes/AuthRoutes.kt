package com.appdist.api.routes

import com.appdist.api.dto.*
import com.appdist.domain.service.AuthService
import com.appdist.domain.service.InvalidCredentialsException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(authService: AuthService) {
    route("/auth") {
        post("/login") {
            val req = runCatching { call.receive<LoginRequest>() }.getOrElse {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_REQUEST", "email and password required"))
                return@post
            }
            if (req.email.isBlank() || req.password.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_REQUEST", "email and password required"))
                return@post
            }
            val tokens = runCatching { authService.login(req.email.trim(), req.password) }
                .getOrElse { e ->
                    when (e) {
                        is InvalidCredentialsException -> call.respond(
                            HttpStatusCode.Unauthorized,
                            ErrorResponse("INVALID_CREDENTIALS", "Invalid email or password")
                        )
                        else -> call.respond(
                            HttpStatusCode.InternalServerError,
                            ErrorResponse("SERVER_ERROR", "Unexpected error")
                        )
                    }
                    return@post
                }
            call.respond(HttpStatusCode.OK, AuthResponse(tokens.accessToken, tokens.refreshToken))
        }

        post("/refresh") {
            val req = call.receive<RefreshTokenRequest>()
            val tokens = runCatching { authService.refreshToken(req.refreshToken) }
                .getOrElse {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("INVALID_TOKEN", "Invalid refresh token"))
                    return@post
                }
            call.respond(HttpStatusCode.OK, AuthResponse(tokens.accessToken, tokens.refreshToken))
        }
    }
}
