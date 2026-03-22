package com.appdist.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.appdist.core.ui.theme.StatusDeprecated
import com.appdist.core.ui.theme.StatusDeprecatedContainer
import com.appdist.core.ui.theme.StatusExpiring
import com.appdist.core.ui.theme.StatusExpiringContainer
import com.appdist.core.ui.theme.StatusLatest
import com.appdist.core.ui.theme.StatusLatestContainer
import com.appdist.core.ui.theme.StatusMandatory
import com.appdist.core.ui.theme.StatusMandatoryContainer

enum class BuildChipType {
    MANDATORY, LATEST, DEPRECATED, EXPIRING, CHANNEL, ENVIRONMENT
}

@Composable
fun BuildStatusChip(
    label: String,
    type: BuildChipType,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (type) {
        BuildChipType.MANDATORY -> StatusMandatoryContainer to StatusMandatory
        BuildChipType.LATEST -> StatusLatestContainer to StatusLatest
        BuildChipType.DEPRECATED -> StatusDeprecatedContainer to StatusDeprecated
        BuildChipType.EXPIRING -> StatusExpiringContainer to StatusExpiring
        BuildChipType.CHANNEL, BuildChipType.ENVIRONMENT ->
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }
    Text(
        text = label.uppercase(),
        color = fg,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
            .background(bg, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}
