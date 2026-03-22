package com.appdist.api.routes

import com.appdist.api.dto.*
import com.appdist.domain.repository.AuditRepository
import com.appdist.domain.repository.ProjectRepository
import com.appdist.domain.repository.WorkspaceRepository
import com.appdist.plugins.AuthPrincipal
import com.appdist.plugins.JWT_AUTH
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

private val log = KotlinLogging.logger {}
private val routeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

fun Route.projectRoutes(
    projectRepo: ProjectRepository,
    auditRepo: AuditRepository,
    workspaceRepo: WorkspaceRepository,
) {
    authenticate(JWT_AUTH) {

        // Android client: workspaceId from JWT (no path param)
        get("/projects") {
            val principal = call.principal<AuthPrincipal>()!!
            val workspaceId = try {
                UUID.fromString(principal.workspaceId)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_ID", "Invalid workspace UUID in token"))
                return@get
            }
            workspaceRepo.findById(workspaceId)
                ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse("WORKSPACE_NOT_FOUND", "Workspace not found")
                )
            val projects = projectRepo.listByWorkspace(workspaceId)
            call.respond(projects.map { it.toDto() })
        }

        route("/workspaces/{workspaceId}/projects") {
            get {
                val workspaceId = try {
                    UUID.fromString(call.parameters["workspaceId"]!!)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_ID", "Invalid UUID format"))
                    return@get
                }
                val projects = projectRepo.listByWorkspace(workspaceId)
                call.respond(projects.map { it.toDto() })
            }
            post {
                val workspaceId = try {
                    UUID.fromString(call.parameters["workspaceId"]!!)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_ID", "Invalid UUID format"))
                    return@post
                }
                val principal = call.principal<AuthPrincipal>()!!
                val req = call.receive<CreateProjectRequest>()
                val project = projectRepo.create(workspaceId, req.name, req.packageName)
                routeScope.launch {
                    runCatching {
                        auditRepo.log(
                            userId = UUID.fromString(principal.userId),
                            action = "project.create",
                            resourceType = "project",
                            resourceId = project.id,
                            metadata = mapOf("name" to project.name)
                        )
                    }.onFailure { log.warn(it) { "Audit log failed for project.create" } }
                }
                call.respond(HttpStatusCode.Created, project.toDto())
            }
        }

        route("/projects/{projectId}") {
            get {
                val id = try {
                    UUID.fromString(call.parameters["projectId"]!!)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_ID", "Invalid UUID format"))
                    return@get
                }
                val project = projectRepo.findById(id)
                    ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", "Project not found"))
                call.respond(project.toDto())
            }
            delete {
                val id = try {
                    UUID.fromString(call.parameters["projectId"]!!)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("INVALID_ID", "Invalid UUID format"))
                    return@delete
                }
                val principal = call.principal<AuthPrincipal>()!!
                projectRepo.delete(id)
                routeScope.launch {
                    runCatching {
                        auditRepo.log(
                            userId = UUID.fromString(principal.userId),
                            action = "project.delete",
                            resourceType = "project",
                            resourceId = id
                        )
                    }.onFailure { log.warn(it) { "Audit log failed for project.delete" } }
                }
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private fun com.appdist.domain.model.Project.toDto() = ProjectDto(
    id = id.toString(),
    workspaceId = workspaceId.toString(),
    name = name,
    packageName = packageName,
    iconUrl = iconUrl,
    createdAt = createdAt.toString(),
)
