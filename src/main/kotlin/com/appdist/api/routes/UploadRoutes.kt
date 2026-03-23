package com.appdist.api.routes

import com.appdist.api.dto.ErrorResponse
import com.appdist.api.dto.toDto
import com.appdist.domain.model.BuildEnvironment
import com.appdist.domain.model.ReleaseChannel
import com.appdist.domain.model.UserRole
import com.appdist.domain.service.BuildService
import com.appdist.domain.service.UploadRequest
import com.appdist.plugins.AuthPrincipal
import com.appdist.plugins.JWT_AUTH
import com.appdist.plugins.requireRole
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
            call.requireRole(UserRole.ADMIN, UserRole.UPLOADER)
            val principal = call.principal<AuthPrincipal>()!!
            val multipart = call.receiveMultipart(formFieldLimit = 500L * 1024 * 1024)

            var apkFile: File? = null
            var originalFileName: String = "app.apk"
            var projectIdRaw: String? = null
            var environmentRaw = "QA"
            var channelRaw = "INTERNAL"
            var buildType = "debug"
            var flavor: String? = null
            var branch: String? = null
            var commitHash: String? = null
            var changelog: String? = null

            try {
                runCatching { multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FormItem -> {
                            when (part.name) {
                                "projectId" -> projectIdRaw = part.value
                                "environment" -> environmentRaw = part.value.uppercase()
                                "channel" -> channelRaw = part.value.uppercase()
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
                                originalFileName = part.originalFileName ?: "app.apk"
                            }
                        }
                        else -> {}
                    }
                    part.dispose()
                } }.getOrElse {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("UPLOAD_FAILED", it.message ?: "Failed to read multipart"))
                    return@post
                }

                val file = apkFile
                val pid = projectIdRaw?.let {
                    try {
                        UUID.fromString(it)
                    } catch (e: IllegalArgumentException) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse("INVALID_ID", "Invalid UUID format for projectId")
                        )
                        return@post
                    }
                }

                if (file == null || pid == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("MISSING_FIELDS", "apk file and projectId are required")
                    )
                    return@post
                }

                val environment = runCatching { BuildEnvironment.valueOf(environmentRaw) }.getOrElse {
                    apkFile?.delete()
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("INVALID_FIELD", "Invalid environment: $environmentRaw")
                    )
                    return@post
                }

                val channel = runCatching { ReleaseChannel.valueOf(channelRaw) }.getOrElse {
                    apkFile?.delete()
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("INVALID_FIELD", "Invalid channel: $channelRaw")
                    )
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
                            originalFileName = originalFileName
                        )
                    )
                }.getOrElse {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse("UPLOAD_FAILED", it.message ?: "Upload failed")
                    )
                    return@post
                }

                call.respond(HttpStatusCode.Created, build.toDto())
            } finally {
                apkFile?.delete()
            }
        }
    }
}
