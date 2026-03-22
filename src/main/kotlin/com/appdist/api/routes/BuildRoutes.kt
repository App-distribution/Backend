package com.appdist.api.routes

import com.appdist.api.dto.*
import com.appdist.domain.model.BuildStatus
import com.appdist.domain.model.ReleaseChannel
import com.appdist.domain.service.BuildService
import com.appdist.plugins.AuthPrincipal
import com.appdist.plugins.JWT_AUTH
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.buildRoutes(buildService: BuildService) {
    authenticate(JWT_AUTH) {
        route("/projects/{projectId}/builds") {
            get {
                val projectId = UUID.fromString(call.parameters["projectId"]!!)
                val channel = call.request.queryParameters["channel"]
                    ?.let { runCatching { ReleaseChannel.valueOf(it.uppercase()) }.getOrNull() }
                val search = call.request.queryParameters["search"]
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
                val limit = (call.request.queryParameters["limit"]?.toIntOrNull() ?: 20).coerceIn(1, 100)

                val builds = buildService.listBuilds(projectId, channel, search, page, limit)
                call.respond(BuildListResponse(
                    builds = builds.map { it.toDto() },
                    total = builds.size, page = page, limit = limit
                ))
            }
        }

        route("/builds/{buildId}") {
            get {
                val buildId = UUID.fromString(call.parameters["buildId"]!!)
                val build = runCatching { buildService.getBuild(buildId) }.getOrElse {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", "Build not found"))
                    return@get
                }
                call.respond(build.toDto())
            }

            patch {
                val buildId = UUID.fromString(call.parameters["buildId"]!!)
                val principal = call.principal<AuthPrincipal>()!!
                val req = call.receive<UpdateBuildRequest>()
                val status = req.status?.let {
                    runCatching { BuildStatus.valueOf(it.uppercase()) }.getOrElse {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_STATUS", "Unknown build status"))
                        return@patch
                    }
                } ?: BuildStatus.ACTIVE
                val build = buildService.updateBuild(buildId, req.changelog, status, UUID.fromString(principal.userId))
                call.respond(build.toDto())
            }

            delete {
                val buildId = UUID.fromString(call.parameters["buildId"]!!)
                val principal = call.principal<AuthPrincipal>()!!
                runCatching { buildService.deleteBuild(buildId, UUID.fromString(principal.userId)) }.getOrElse {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", "Build not found"))
                    return@delete
                }
                call.respond(HttpStatusCode.NoContent)
            }

            get("/download-url") {
                val buildId = UUID.fromString(call.parameters["buildId"]!!)
                val principal = call.principal<AuthPrincipal>()!!
                val url = runCatching {
                    buildService.getDownloadUrl(buildId, UUID.fromString(principal.userId))
                }.getOrElse {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", it.message ?: "Build not found"))
                    return@get
                }
                call.respond(DownloadUrlResponse(url))
            }
        }
    }
}
