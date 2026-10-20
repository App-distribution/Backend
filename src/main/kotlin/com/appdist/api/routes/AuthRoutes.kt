package com.appdist.api.routes

import com.appdist.api.dto.*
import com.appdist.domain.service.AuthService
import com.appdist.domain.service.EmailDeliveryException
import com.appdist.domain.service.OtpRateLimiter
import com.appdist.domain.service.RateLimitDecision
import io.ktor.server.plugins.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(authService: AuthService, otpRateLimiter: OtpRateLimiter = OtpRateLimiter()) {
    route("/auth") {
        post("/request-otp") {
            val req = call.receive<RequestOtpRequest>()
            if (req.email.isBlank() || !req.email.contains("@")) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_EMAIL", "Valid email required"))
                return@post
            }
            val limit = otpRateLimiter.tryAcquire(req.email, call.clientIp())
            if (limit is RateLimitDecision.Limited) {
                call.response.header(HttpHeaders.RetryAfter, limit.retryAfterSeconds.toString())
                call.respond(
                    HttpStatusCode.TooManyRequests,
                    ErrorResponse(
                        "TOO_MANY_REQUESTS",
                        "Слишком много запросов кода. Повторите через ${limit.retryAfterSeconds} сек.",
                    ),
                )
                return@post
            }
            try {
                authService.requestOtp(req.email)
            } catch (_: EmailDeliveryException) {
                call.respond(
                    HttpStatusCode.ServiceUnavailable,
                    ErrorResponse("EMAIL_SEND_FAILED", "Не удалось отправить код на почту. Попробуйте позже."),
                )
                return@post
            }
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

/**
 * Traefik перед бэкендом выставляет X-Forwarded-For сам и не доверяет
 * пришедшему от клиента, а при цепочке прокси последний адрес — тот, кого
 * видел ближайший к нам прокси. Без заголовка (локально, тесты) — адрес сокета.
 */
private fun ApplicationCall.clientIp(): String =
    request.headers[HttpHeaders.XForwardedFor]
        ?.split(',')?.lastOrNull()?.trim()?.takeIf { it.isNotEmpty() }
        ?: request.origin.remoteHost
