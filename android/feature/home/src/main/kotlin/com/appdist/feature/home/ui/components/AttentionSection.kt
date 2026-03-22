package com.appdist.feature.home.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appdist.feature.home.domain.model.AttentionItem

@Composable
fun AttentionSection(
    items: List<AttentionItem>,
    onBuildClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Требует внимания", style = MaterialTheme.typography.titleMedium)
        items.forEach { item ->
            AttentionItemCard(item, onBuildClick)
        }
    }
}

@Composable
fun AttentionItemCard(item: AttentionItem, onBuildClick: (String) -> Unit) {
    when (item) {
        is AttentionItem.MandatoryUpdate -> MandatoryCard(item, onBuildClick)
        is AttentionItem.ExpiringBuild -> ExpiringCard(item, onBuildClick)
        is AttentionItem.NewBuildInSubscribedChannel -> NewBuildCard(item, onBuildClick)
    }
}

@Composable
private fun MandatoryCard(item: AttentionItem.MandatoryUpdate, onBuildClick: (String) -> Unit) {
    ElevatedCard(
        onClick = { onBuildClick(item.build.id) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Обязательное обновление",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(4.dp))
            Text(item.build.versionName, style = MaterialTheme.typography.bodyLarge)
            Text(item.build.channel, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { onBuildClick(item.build.id) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Обновить") }
            }
        }
    }
}

@Composable
private fun ExpiringCard(item: AttentionItem.ExpiringBuild, onBuildClick: (String) -> Unit) {
    ElevatedCard(
        onClick = { onBuildClick(item.build.id) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Истекает через ${item.daysLeft} дн.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(Modifier.height(4.dp))
            Text(item.build.versionName, style = MaterialTheme.typography.bodyLarge)
            Text(item.build.channel, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { onBuildClick(item.build.id) }) { Text("Посмотреть") }
            }
        }
    }
}

@Composable
private fun NewBuildCard(item: AttentionItem.NewBuildInSubscribedChannel, onBuildClick: (String) -> Unit) {
    ElevatedCard(
        onClick = { onBuildClick(item.build.id) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Новая сборка",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(item.build.versionName, style = MaterialTheme.typography.bodyLarge)
            Text(item.build.channel, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { onBuildClick(item.build.id) }) { Text("Посмотреть") }
            }
        }
    }
}
