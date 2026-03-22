package com.appdist.feature.builddetail.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appdist.core.common.model.InstallStatus
import com.appdist.core.ui.components.ErrorScreen
import com.appdist.feature.builddetail.domain.DownloadState
import com.appdist.core.ui.components.LoadingScreen
import com.appdist.feature.builddetail.ui.components.BuildHeroSection
import com.appdist.feature.builddetail.ui.components.ChangelogSection
import com.appdist.feature.builddetail.ui.components.TechDetailsSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildDetailScreen(
    buildId: String,
    onBack: () -> Unit,
    viewModel: BuildDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is BuildDetailEffect.LaunchInstaller -> {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(effect.apkUri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
                is BuildDetailEffect.CopyToClipboard ->
                    clipboard.setText(AnnotatedString(effect.text))
                is BuildDetailEffect.ShowSnackbar -> { /* TODO: SnackbarHostState */ }
                is BuildDetailEffect.ShowSignatureMismatchDialog -> { /* TODO: show dialog */ }
                is BuildDetailEffect.ShowDowngradeWarning -> { /* TODO: show dialog */ }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.build?.let { "${it.projectName} v${it.versionName}" } ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onAction(BuildDetailAction.CopyLinkClicked) }) {
                        Icon(Icons.Default.Share, "Copy link")
                    }
                }
            )
        },
        bottomBar = {
            state.build?.let { build ->
                StickyInstallBar(
                    build = build,
                    downloadState = state.downloadState,
                    onAction = viewModel::onAction
                )
            }
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingScreen(modifier = Modifier.padding(padding))
            state.error != null -> ErrorScreen(
                error = state.error!!,
                onRetry = { viewModel.onAction(BuildDetailAction.RetryClicked) },
                modifier = Modifier.padding(padding)
            )
            state.build != null -> {
                val build = state.build!!
                Column(
                    Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                ) {
                    BuildHeroSection(build, Modifier.padding(16.dp))
                    build.changelog?.takeIf { it.isNotBlank() }?.let { changelog ->
                        ChangelogSection(changelog, Modifier.padding(horizontal = 16.dp))
                    }
                    TechDetailsSection(build, Modifier.padding(16.dp))
                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun StickyInstallBar(
    build: com.appdist.core.common.model.BuildUi,
    downloadState: DownloadState,
    onAction: (BuildDetailAction) -> Unit
) {
    Surface(shadowElevation = 8.dp) {
        Column(Modifier.padding(16.dp)) {
            if (downloadState is DownloadState.Downloading) {
                LinearProgressIndicator(
                    progress = { downloadState.progress },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )
                Text(
                    "${downloadState.bytesLoaded / 1024 / 1024} MB скачано",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(4.dp))
            }

            val (buttonText, enabled) = when {
                downloadState is DownloadState.Downloading -> "Отмена" to true
                downloadState is DownloadState.Verifying -> "Проверка..." to false
                downloadState is DownloadState.ReadyToInstall -> "Установить" to true
                build.installStatus is InstallStatus.NotInstalled -> "Скачать и установить" to true
                build.installStatus is InstallStatus.UpdateAvailable -> "Обновить до v${build.versionName}" to true
                build.installStatus is InstallStatus.Installed -> "Установлена актуальная версия" to false
                build.installStatus is InstallStatus.InstalledNewer -> "Откатить до v${build.versionName}" to true
                build.installStatus is InstallStatus.Incompatible -> "Несовместимо с устройством" to false
                build.installStatus is InstallStatus.SignatureMismatch -> "Другая подпись — установить?" to true
                else -> "Скачать" to true
            }

            Button(
                onClick = {
                    if (downloadState is DownloadState.Downloading)
                        onAction(BuildDetailAction.CancelDownload)
                    else
                        onAction(BuildDetailAction.DownloadOrInstallClicked)
                },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            ) { Text(buttonText) }
        }
    }
}
