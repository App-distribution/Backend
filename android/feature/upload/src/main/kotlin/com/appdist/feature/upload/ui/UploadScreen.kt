package com.appdist.feature.upload.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    onSuccess: () -> Unit,
    viewModel: UploadViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is UploadEffect.UploadSuccess -> onSuccess()
                is UploadEffect.ShowError -> { /* shown via state.error */ }
            }
        }
    }

    val apkPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onAction(UploadAction.ApkSelected(it)) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Загрузить APK") }) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // APK picker button
            OutlinedButton(
                onClick = { apkPickerLauncher.launch("application/vnd.android.package-archive") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AttachFile, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(state.selectedApkUri?.lastPathSegment ?: "Выбрать APK файл")
            }

            // Metadata display
            state.extractedMetadata?.let { meta ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(meta.packageName, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "v${meta.versionName} (${meta.versionCode})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "minSdk ${meta.minSdk} / targetSdk ${meta.targetSdk}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Channel selector
            var channelExpanded by remember { mutableStateOf(false) }
            val channels = listOf("internal", "alpha", "beta", "rc")
            ExposedDropdownMenuBox(
                expanded = channelExpanded,
                onExpandedChange = { channelExpanded = it }
            ) {
                OutlinedTextField(
                    value = state.channel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Channel") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = channelExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = channelExpanded,
                    onDismissRequest = { channelExpanded = false }
                ) {
                    channels.forEach { ch ->
                        DropdownMenuItem(
                            text = { Text(ch) },
                            onClick = {
                                viewModel.onAction(UploadAction.ChannelChanged(ch))
                                channelExpanded = false
                            }
                        )
                    }
                }
            }

            // Changelog
            OutlinedTextField(
                value = state.changelog,
                onValueChange = { viewModel.onAction(UploadAction.ChangelogChanged(it)) },
                label = { Text("Changelog") },
                placeholder = { Text("Что изменилось в этой версии...") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                maxLines = 5
            )

            // Error
            state.error?.let { error ->
                Text(
                    text = when (error) {
                        is com.appdist.core.common.AppError.Unknown -> error.message
                        else -> "Ошибка: $error"
                    },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Progress
            state.uploadProgress?.let { progress ->
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            // Upload button
            Button(
                onClick = { viewModel.onAction(UploadAction.UploadClicked) },
                enabled = state.extractedMetadata != null && state.uploadProgress == null && !state.isSuccess,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.isSuccess) "Загружено!" else "Загрузить")
            }
        }
    }
}
