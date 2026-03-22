package com.appdist.feature.browse.ui.builds.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appdist.feature.browse.ui.builds.BuildFilters

val CHANNELS = listOf("alpha", "beta", "rc", "internal", "nightly")
val ENVIRONMENTS = listOf("dev", "qa", "staging")

@Composable
fun FilterChipsRow(
    filters: BuildFilters,
    onChannelSelected: (String?) -> Unit,
    onEnvironmentSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.horizontalScroll(rememberScrollState())) {
        CHANNELS.forEach { channel ->
            FilterChip(
                selected = filters.channel == channel,
                onClick = { onChannelSelected(if (filters.channel == channel) null else channel) },
                label = { Text(channel) }
            )
            Spacer(Modifier.width(4.dp))
        }
        Spacer(Modifier.width(8.dp))
        ENVIRONMENTS.forEach { env ->
            FilterChip(
                selected = filters.environment == env,
                onClick = { onEnvironmentSelected(if (filters.environment == env) null else env) },
                label = { Text(env) }
            )
            Spacer(Modifier.width(4.dp))
        }
    }
}
