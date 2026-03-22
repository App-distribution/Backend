package com.appdist.feature.builddetail.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ChangelogSection(changelog: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text("Что нового", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(changelog, style = MaterialTheme.typography.bodyMedium)
    }
}
