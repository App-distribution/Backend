package com.appdist.feature.settings.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.WorkManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AppDistFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var workManager: WorkManager

    override fun onNewToken(token: String) {
        // TODO: send token to backend via UpdateMe API
    }

    override fun onMessageReceived(message: RemoteMessage) {
        if (message.data.containsKey("new_build")) {
            SyncBuildsWorker.schedule(workManager)
        }
        message.notification?.let { notification ->
            showNotification(
                title = notification.title ?: "AppDistribution",
                body = notification.body ?: "Новая сборка доступна",
                buildId = message.data["build_id"]
            )
        }
    }

    private fun showNotification(title: String, body: String, buildId: String?) {
        val channelId = "appdist_builds"
        val nm = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "Новые сборки", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }

        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            buildId?.let { putExtra("build_id", it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notif = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(System.currentTimeMillis().toInt(), notif)
    }
}
