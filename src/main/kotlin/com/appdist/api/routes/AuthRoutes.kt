package com.appdist.api.routes

import com.appdist.api.dto.*
import com.appdist.domain.service.AuthService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(authService: AuthService) {
    route("/auth") {
        post("/request-otp") {
            val req = call.receive<RequestOtpRequest>()
            if (req.email.isBlank() || !req.email.contains("@")) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_EMAIL", "Valid email required"))
                return@post
            }
            authService.requestOtp(req.email)
            call.respond(HttpStatusCode.OK, MessageResponse("OTP sent to ${req.email}"))
        }

        post("/verify-otp") {
            val req = call.receive<VerifyOtpRequest>()
            val tokens = runCatching { authService.verifyOtp(req.email, req.otp) }
                .getOrElse {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        ErrorResponse("INVALID_OTP", it.message ?: "Invalid OTP")
                    )
                    return@post
                }
            call.respond(HttpStatusCode.OK, AuthResponse(tokens.accessToken, tokens.refreshToken))
        }

        post("/refresh") {
            val req = call.receive<RefreshTokenRequest>()
            val tokens = runCatching { authService.refreshToken(req.refreshToken) }
                .getOrElse {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        ErrorResponse("INVALID_TOKEN", "Invalid refresh token")
                    )
                    return@post
                }
            call.respond(HttpStatusCode.OK, AuthResponse(tokens.accessToken, tokens.refreshToken))
        }
    }
}
