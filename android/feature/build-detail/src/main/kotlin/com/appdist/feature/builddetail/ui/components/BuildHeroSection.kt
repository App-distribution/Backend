package com.appdist.feature.builddetail.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.appdist.core.common.model.BuildUi
import com.appdist.core.ui.components.BuildChipType
import com.appdist.core.ui.components.BuildStatusChip

@Composable
fun BuildHeroSection(build: BuildUi, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (build.projectName.isNotBlank()) build.projectName else build.packageName,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    "v${build.versionName} (${build.versionCode})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            BuildStatusChip(build.channel, BuildChipType.CHANNEL)
            BuildStatusChip(build.environment, BuildChipType.ENVIRONMENT)
            if (build.status == "mandatory") BuildStatusChip("MANDATORY", BuildChipType.MANDATORY)
            if (build.isLatestInChannel) BuildStatusChip("LATEST", BuildChipType.LATEST)
        }
    }
}
