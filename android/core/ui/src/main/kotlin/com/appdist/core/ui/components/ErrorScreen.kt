package com.appdist.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appdist.core.common.AppError

@Composable
fun ErrorScreen(
    error: AppError,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = error.toHumanMessage(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (onRetry != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("Повторить") }
        }
    }
}

private fun AppError.toHumanMessage() = when (this) {
    AppError.NoInternet -> "Нет подключения к сети"
    AppError.Unauthorized -> "Необходима авторизация"
    is AppError.Network -> "Ошибка сервера ($code)"
    is AppError.ChecksumMismatch -> "Файл повреждён — checksum не совпадает"
    is AppError.SignatureMismatch -> "Подпись приложения отличается от установленной"
    is AppError.IncompatibleDevice -> "Устройство не поддерживает эту сборку"
    is AppError.Storage -> "Ошибка хранилища: $message"
    is AppError.Unknown -> message
}
