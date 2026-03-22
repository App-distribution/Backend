package com.appdist.feature.builddetail.ui

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import com.appdist.core.common.model.BuildUi
import com.appdist.core.common.model.InstallStatus
import com.appdist.feature.builddetail.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class BuildDetailUiState(
    val build: BuildUi? = null,
    val downloadState: DownloadState = DownloadState.Idle,
    val isLoading: Boolean = true,
    val error: AppError? = null
)

sealed interface BuildDetailAction {
    data object DownloadOrInstallClicked : BuildDetailAction
    data object CancelDownload : BuildDetailAction
    data object CopyLinkClicked : BuildDetailAction
    data object RetryClicked : BuildDetailAction
}

sealed interface BuildDetailEffect {
    data class LaunchInstaller(val apkUri: Uri) : BuildDetailEffect
    data class ShowSnackbar(val message: String) : BuildDetailEffect
    data class CopyToClipboard(val text: String) : BuildDetailEffect
    data class ShowSignatureMismatchDialog(val installedFp: String) : BuildDetailEffect
    data class ShowDowngradeWarning(val installedCode: Long, val newCode: Long) : BuildDetailEffect
}

@HiltViewModel
class BuildDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val getBuildDetail: GetBuildDetailUseCase,
    private val getInstallStatus: GetInstallStatusUseCase,
    private val downloadBuild: DownloadBuildUseCase,
    private val reportInstall: ReportInstallUseCase
) : ViewModel() {

    val buildId: String = checkNotNull(savedStateHandle["buildId"])

    private val _state = MutableStateFlow(BuildDetailUiState())
    val state: StateFlow<BuildDetailUiState> = _state.asStateFlow()

    private val _effects = Channel<BuildDetailEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init { loadBuild() }

    fun onAction(action: BuildDetailAction) = when (action) {
        BuildDetailAction.DownloadOrInstallClicked -> handleInstallAction()
        BuildDetailAction.CancelDownload -> cancelDownload()
        BuildDetailAction.CopyLinkClicked -> copyLink()
        BuildDetailAction.RetryClicked -> loadBuild()
    }

    private fun loadBuild() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            getBuildDetail(buildId)
                .catch { e ->
                    _state.update { it.copy(isLoading = false, error = AppError.Unknown(e.message ?: "Error")) }
                }
                .collect { build ->
                    val status = getInstallStatus(build.packageName, build.versionCode, build.certFingerprint, build.minSdk)
                    _state.update { it.copy(build = build.copy(installStatus = status), isLoading = false) }
                }
        }
    }

    private fun handleInstallAction() {
        val build = _state.value.build ?: return
        val downloadState = _state.value.downloadState

        if (downloadState is DownloadState.ReadyToInstall) {
            launchInstaller(downloadState.filePath)
            return
        }

        val status = build.installStatus
        if (status is InstallStatus.SignatureMismatch) {
            viewModelScope.launch {
                _effects.send(BuildDetailEffect.ShowSignatureMismatchDialog(status.installedFingerprint))
            }
            return
        }

        if (status is InstallStatus.InstalledNewer) {
            viewModelScope.launch {
                _effects.send(BuildDetailEffect.ShowDowngradeWarning(status.installed, status.available))
            }
            return
        }

        startDownload(build)
    }

    private fun startDownload(build: BuildUi) {
        viewModelScope.launch {
            val urlResult = getBuildDetail.getDownloadUrl(buildId)
            if (urlResult is Result.Error) {
                _effects.send(BuildDetailEffect.ShowSnackbar("Не удалось получить ссылку на скачивание"))
                return@launch
            }
            val url = (urlResult as Result.Success).data

            downloadBuild(build, url).collect { dlState ->
                _state.update { it.copy(downloadState = dlState) }
                if (dlState is DownloadState.ReadyToInstall) launchInstaller(dlState.filePath)
                if (dlState is DownloadState.Failed) {
                    _effects.send(BuildDetailEffect.ShowSnackbar("Ошибка загрузки"))
                }
            }
        }
    }

    private fun launchInstaller(filePath: String) {
        viewModelScope.launch {
            reportInstall(buildId)
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                File(filePath)
            )
            _effects.send(BuildDetailEffect.LaunchInstaller(apkUri))
        }
    }

    private fun cancelDownload() {
        // WorkManager cancel
    }

    private fun copyLink() {
        viewModelScope.launch {
            _effects.send(BuildDetailEffect.CopyToClipboard("appdist://builds/$buildId"))
        }
    }
}
