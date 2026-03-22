package com.appdist.feature.browse.ui.builds.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appdist.core.common.model.BuildUi
import com.appdist.core.common.model.InstallStatus
import com.appdist.core.ui.components.BuildChipType
import com.appdist.core.ui.components.BuildStatusChip
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BuildCard(
    build: BuildUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateStr = remember(build.uploadDate) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            .format(Date(build.uploadDate))
    }

    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${build.projectName} v${build.versionName}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "(${build.versionCode}) · $dateStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                when (build.installStatus) {
                    is InstallStatus.UpdateAvailable -> Badge { Text("↑") }
                    is InstallStatus.Installed -> Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Installed",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    else -> Unit
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                BuildStatusChip(build.channel, BuildChipType.CHANNEL)
                if (build.status == "mandatory") BuildStatusChip("MANDATORY", BuildChipType.MANDATORY)
                if (build.isLatestInChannel) BuildStatusChip("LATEST", BuildChipType.LATEST)
                if (build.status == "deprecated") BuildStatusChip("DEPRECATED", BuildChipType.DEPRECATED)
            }

            Spacer(Modifier.height(4.dp))
            Text(
                "${build.fileSize / 1024 / 1024} MB · ${build.uploaderName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
