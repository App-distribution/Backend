package com.appdist.plugins

import com.appdist.api.dto.ErrorResponse
import com.appdist.config.AppConfig
import com.appdist.domain.model.UserRole
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*

const val JWT_AUTH = "jwt-auth"

data class AuthPrincipal(
    val userId: String,
    val email: String,
    val role: UserRole,
    val workspaceId: String,
) : Principal

fun Application.configureAuth(jwtConfig: AppConfig.JwtConfig) {
    install(Authentication) {
        jwt(JWT_AUTH) {
            val algorithm = Algorithm.HMAC256(jwtConfig.secret)
            verifier(
                JWT.require(algorithm)
                    .withIssuer(jwtConfig.issuer)
                    .withAudience(jwtConfig.audience)
                    .build()
            )
            validate { credential ->
                val userId = credential.payload.subject ?: return@validate null
                val email = credential.payload.getClaim("email").asString() ?: return@validate null
                val roleStr = credential.payload.getClaim("role").asString() ?: return@validate null
                val workspaceId = credential.payload.getClaim("workspace_id").asString() ?: return@validate null
                val role = runCatching { UserRole.valueOf(roleStr) }.getOrNull() ?: return@validate null
                AuthPrincipal(userId, email, role, workspaceId)
            }
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse("UNAUTHORIZED", "Token is not valid or expired")
                )
            }
        }
    }
}
