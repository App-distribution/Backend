package com.appdist.domain.service

import com.appdist.domain.model.Build
import com.appdist.domain.model.UserRole
import com.appdist.domain.repository.UserRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.*
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID

private val log = KotlinLogging.logger {}

class NotificationService(
    private val userRepository: UserRepository,
) {
    val isEnabled get() = FirebaseApp.getApps().isNotEmpty()

    suspend fun notifyNewBuild(build: Build, projectName: String, workspaceId: UUID) {
        if (!isEnabled) {
            log.warn { "FCM disabled — skipping notifyNewBuild for build ${build.id}" }
            return
        }

        val users = userRepository.findAllByWorkspace(workspaceId)
        val tokens = users
            .filter { it.role in listOf(UserRole.ADMIN, UserRole.UPLOADER, UserRole.TESTER) }
            .mapNotNull { it.fcmToken }

        if (tokens.isEmpty()) {
            log.debug { "No FCM tokens in workspace $workspaceId, skipping notification" }
            return
        }

        val message = MulticastMessage.builder()
            .addAllTokens(tokens)
            .putData("new_build", "true")
            .putData("build_id", build.id.toString())
            .setNotification(
                Notification.builder()
                    .setTitle("New build: ${build.versionName}")
                    .setBody("$projectName • ${build.channel.name}")
                    .build()
            )
            .build()

        val result = FirebaseMessaging.getInstance().sendEachForMulticast(message)
        log.info { "FCM sent: ${result.successCount} success, ${result.failureCount} failure" }
    }
}
