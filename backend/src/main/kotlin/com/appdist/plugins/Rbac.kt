package com.appdist.plugins

import com.appdist.domain.model.UserRole
import io.ktor.server.application.*
import io.ktor.server.auth.*

/**
 * Throws UnauthorizedException if no principal, ForbiddenException if role not in [roles].
 * Usage: call.requireRole(UserRole.ADMIN, UserRole.UPLOADER)
 */
fun ApplicationCall.requireRole(vararg roles: UserRole) {
    val principal = principal<AuthPrincipal>()
        ?: throw UnauthorizedException()
    if (principal.role !in roles) throw ForbiddenException()
}
