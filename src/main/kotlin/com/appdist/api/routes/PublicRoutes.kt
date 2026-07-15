package com.appdist.api.routes

import com.appdist.api.dto.DownloadUrlResponse
import com.appdist.api.dto.ErrorResponse
import com.appdist.api.dto.toDto
import com.appdist.domain.model.BuildStatus
import com.appdist.domain.model.ReleaseChannel
import com.appdist.domain.repository.ProjectRepository
import com.appdist.domain.service.BuildService
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
private data class PublicProjectDto(
    val id: String,
    val workspaceId: String,
    val name: String,
    val packageName: String,
    val iconUrl: String?,
    val createdAt: Long,
)

fun Route.publicRoutes(projectRepository: ProjectRepository, buildService: BuildService) {
    route("/public") {
        get("/projects") {
            val projects = projectRepository.listAll().map {
                PublicProjectDto(
                    id = it.id.toString(),
                    workspaceId = it.workspaceId.toString(),
                    name = it.name,
                    packageName = it.packageName,
                    iconUrl = it.iconUrl,
                    createdAt = it.createdAt.toEpochMilliseconds(),
                )
            }
            call.respond(projects)
        }

        get("/projects/{projectId}/builds") {
            val projectId = call.parameters["projectId"]?.let {
                runCatching { UUID.fromString(it) }.getOrNull()
            } ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("INVALID_ID", "Invalid UUID format"),
            )

            projectRepository.findById(projectId)
                ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", "Project not found"))

            val channel = call.request.queryParameters["channel"]
                ?.let { runCatching { ReleaseChannel.valueOf(it.uppercase()) }.getOrNull() }
            val search = call.request.queryParameters["search"]
            val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0
            val limit = (call.request.queryParameters["limit"]?.toIntOrNull() ?: 20).coerceIn(1, 100)
            val now = Clock.System.now()
            val builds = buildService.listBuilds(projectId, channel, search, page, limit)
                .filter { build ->
                    (build.status == BuildStatus.ACTIVE || build.status == BuildStatus.MANDATORY) &&
                        (build.expiryDate == null || build.expiryDate > now)
                }
            call.respond(builds.map { it.toDto() })
        }

        get("/builds/{buildId}") {
            val buildId = call.parameters["buildId"]?.let {
                runCatching { UUID.fromString(it) }.getOrNull()
            } ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("INVALID_ID", "Invalid UUID format"),
            )

            val build = runCatching { buildService.getPublicBuild(buildId) }.getOrElse {
                return@get call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse("NOT_FOUND", it.message ?: "Build not found"),
                )
            }
            call.respond(build.toDto())
        }

        get("/builds/{buildId}/download-url") {
            val buildId = call.parameters["buildId"]?.let {
                runCatching { UUID.fromString(it) }.getOrNull()
            } ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse("INVALID_ID", "Invalid UUID format"),
            )

            val url = runCatching { buildService.getPublicDownloadUrl(buildId) }.getOrElse {
                return@get call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse("NOT_FOUND", it.message ?: "Build not found"),
                )
            }
            val expiresAt = Clock.System.now().toEpochMilliseconds() + 15 * 60 * 1000L
            call.respond(DownloadUrlResponse(url, expiresAt))
        }
    }
}
