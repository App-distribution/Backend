package com.appdist.plugins

import com.appdist.api.dto.ErrorResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

class NotFoundException(message: String) : Exception(message)
class ForbiddenException(message: String = "Access denied") : Exception(message)
class UnauthorizedException(message: String = "Unauthorized") : Exception(message)
class ConflictException(message: String) : Exception(message)
class BadRequestException(message: String) : Exception(message)

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<NotFoundException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", cause.message ?: "Not found"))
        }
        exception<ForbiddenException> { call, cause ->
            call.respond(HttpStatusCode.Forbidden, ErrorResponse("FORBIDDEN", cause.message ?: "Forbidden"))
        }
        exception<UnauthorizedException> { call, cause ->
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("UNAUTHORIZED", cause.message ?: "Unauthorized"))
        }
        exception<ConflictException> { call, cause ->
            call.respond(HttpStatusCode.Conflict, ErrorResponse("CONFLICT", cause.message ?: "Conflict"))
        }
        exception<BadRequestException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", cause.message ?: "Bad request"))
        }
        exception<io.ktor.server.plugins.BadRequestException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", cause.message ?: "Bad request"))
        }
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("INTERNAL_ERROR", "Internal server error")
            )
        }
    }
}
