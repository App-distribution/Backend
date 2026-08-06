package com.appdist.plugins

import com.appdist.api.dto.ErrorResponse
import com.appdist.config.AppConfig
import com.appdist.domain.model.UserRole
import com.appdist.domain.service.ApiKeyService
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.auth.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*

const val JWT_AUTH = "jwt-auth"
const val API_KEY_AUTH = "api-key-auth"

data class AuthPrincipal(
    val userId: String,
    val email: String,
    val role: UserRole,
    val workspaceId: String,
) : Principal

fun Application.configureAuth(
    jwtConfig: AppConfig.JwtConfig,
    apiKeyService: ApiKeyService? = null,
) {
    install(Authentication) {
        jwt(JWT_AUTH) {
            val algorithm = Algorithm.HMAC256(jwtConfig.secret)
            authHeader { call ->
                call.request.parseAuthorizationHeader()
                    ?.takeIf(::isPlausibleBearerToken)
            }
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

        // Провайдер регистрируется всегда: без сервиса он просто никого не
        // пропускает, зато роуты с authenticate(JWT_AUTH, API_KEY_AUTH)
        // не падают на старте из-за отсутствующего провайдера.
        bearer(API_KEY_AUTH) {
            authenticate { credential ->
                val service = apiKeyService ?: return@authenticate null
                val identity = service.authenticate(credential.token) ?: return@authenticate null
                AuthPrincipal(
                    userId = identity.userId.toString(),
                    email = identity.email,
                    role = UserRole.UPLOADER,
                    workspaceId = identity.workspaceId.toString(),
                )
            }
        }
    }
}

private fun isPlausibleBearerToken(header: HttpAuthHeader): Boolean {
    val bearer = header as? HttpAuthHeader.Single ?: return false
    if (!bearer.authScheme.equals("Bearer", ignoreCase = true)) return false
    val token = bearer.blob.trim()
    return token.isNotBlank() && token.count { it == '.' } == 2
}
