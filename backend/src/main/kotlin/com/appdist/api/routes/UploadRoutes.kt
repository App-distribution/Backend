package com.appdist.api.routes

import com.appdist.api.dto.ErrorResponse
import com.appdist.api.dto.toDto
import com.appdist.domain.model.BuildEnvironment
import com.appdist.domain.model.ReleaseChannel
import com.appdist.domain.service.BuildService
import com.appdist.domain.service.UploadRequest
import com.appdist.plugins.AuthPrincipal
import com.appdist.plugins.JWT_AUTH
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File
import java.util.UUID

fun Route.uploadRoutes(buildService: BuildService) {
    authenticate(JWT_AUTH) {
        post("/builds/upload") {
            val principal = call.principal<AuthPrincipal>()!!
            val multipart = call.receiveMultipart()

            var apkFile: File? = null
            var projectId: UUID? = null
            var environment = BuildEnvironment.QA
            var channel = ReleaseChannel.INTERNAL
            var buildType = "debug"
            var flavor: String? = null
            var branch: String? = null
            var commitHash: String? = null
            var changelog: String? = null

            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FormItem -> {
                        when (part.name) {
                            "projectId" -> projectId = UUID.fromString(part.value)
                            "environment" -> environment = BuildEnvironment.valueOf(part.value.uppercase())
                            "channel" -> channel = ReleaseChannel.valueOf(part.value.uppercase())
                            "buildType" -> buildType = part.value
                            "flavor" -> flavor = part.value
                            "branch" -> branch = part.value
                            "commitHash" -> commitHash = part.value
                            "changelog" -> changelog = part.value
                        }
                    }
                    is PartData.FileItem -> {
                        if (part.name == "apk") {
                            val tempFile = File.createTempFile("upload_", ".apk")
                            part.streamProvider().use { input ->
                                tempFile.outputStream().use { output -> input.copyTo(output) }
                            }
                            apkFile = tempFile
                        }
                    }
                    else -> {}
                }
                part.dispose()
            }

            val file = apkFile
            val pid = projectId

            if (file == null || pid == null) {
                call.respond(HttpStatusCode.BadRequest,
                    ErrorResponse("MISSING_FIELDS", "apk file and projectId are required"))
                return@post
            }

            val build = runCatching {
                buildService.uploadBuild(
                    UploadRequest(
                        projectId = pid,
                        uploaderId = UUID.fromString(principal.userId),
                        environment = environment,
                        channel = channel,
                        buildType = buildType,
                        flavor = flavor,
                        branch = branch,
                        commitHash = commitHash,
                        changelog = changelog,
                        file = file,
                        originalFileName = "app.apk"
                    )
                )
            }.also { file.delete() }.getOrElse {
                call.respond(HttpStatusCode.InternalServerError,
                    ErrorResponse("UPLOAD_FAILED", it.message ?: "Upload failed"))
                return@post
            }

            call.respond(HttpStatusCode.Created, build.toDto())
        }
    }
}
