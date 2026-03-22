package com.appdist.feature.settings.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun PermissionsScreen() {
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Разрешения для установки", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Text(
            "Для установки тестовых APK Android требует разрешение " +
            "«Установка из неизвестных источников» для AppDistribution.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "1. Нажмите кнопку ниже\n" +
            "2. Найдите AppDistribution в списке\n" +
            "3. Включите «Разрешить установку приложений»\n" +
            "4. Вернитесь назад",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Открыть настройки") }
    }
}
