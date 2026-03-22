package com.appdist.feature.upload.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import com.appdist.feature.upload.domain.ApkMetadata
import com.appdist.feature.upload.domain.ExtractApkMetadataUseCase
import com.appdist.feature.upload.domain.UploadBuildUseCase
import com.appdist.feature.upload.domain.UploadState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class UploadUiState(
    val selectedApkUri: Uri? = null,
    val extractedMetadata: ApkMetadata? = null,
    val channel: String = "internal",
    val changelog: String = "",
    val uploadProgress: Float? = null,
    val isSuccess: Boolean = false,
    val error: AppError? = null
)

sealed interface UploadAction {
    data class ApkSelected(val uri: Uri) : UploadAction
    data class ChannelChanged(val channel: String) : UploadAction
    data class ChangelogChanged(val text: String) : UploadAction
    data object UploadClicked : UploadAction
}

sealed interface UploadEffect {
    data object UploadSuccess : UploadEffect
    data class ShowError(val message: String) : UploadEffect
}

@HiltViewModel
class UploadViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val extractMetadata: ExtractApkMetadataUseCase,
    private val uploadBuild: UploadBuildUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(UploadUiState())
    val state: StateFlow<UploadUiState> = _state.asStateFlow()

    private val _effects = Channel<UploadEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onAction(action: UploadAction) = when (action) {
        is UploadAction.ApkSelected -> extractAndSetMetadata(action.uri)
        is UploadAction.ChannelChanged -> _state.update { it.copy(channel = action.channel) }
        is UploadAction.ChangelogChanged -> _state.update { it.copy(changelog = action.text) }
        UploadAction.UploadClicked -> upload()
    }

    private fun extractAndSetMetadata(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(selectedApkUri = uri, extractedMetadata = null, error = null) }
            val tempFile = withContext(Dispatchers.IO) {
                val file = File(context.cacheDir, "upload_preview.apk")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                file
            }
            when (val result = extractMetadata(tempFile)) {
                is Result.Success -> _state.update { it.copy(extractedMetadata = result.data) }
                is Result.Error -> _state.update { it.copy(error = result.error) }
            }
        }
    }

    private fun upload() {
        val uri = _state.value.selectedApkUri ?: return
        val metadata = _state.value.extractedMetadata ?: return
        val tempFile = File(context.cacheDir, "upload_preview.apk")
        if (!tempFile.exists()) return

        _state.update { it.copy(uploadProgress = 0f, error = null) }
        viewModelScope.launch {
            uploadBuild(
                apkPath = tempFile.absolutePath,
                projectId = "",  // MVP: no project selection — server assigns
                channel = _state.value.channel,
                changelog = _state.value.changelog
            ).collect { uploadState ->
                when (uploadState) {
                    is UploadState.Uploading ->
                        _state.update { it.copy(uploadProgress = uploadState.progress) }
                    is UploadState.Success -> {
                        _state.update { it.copy(isSuccess = true, uploadProgress = null) }
                        _effects.send(UploadEffect.UploadSuccess)
                    }
                    is UploadState.Failed -> {
                        _state.update { it.copy(uploadProgress = null, error = uploadState.error) }
                        _effects.send(UploadEffect.ShowError("Ошибка загрузки"))
                    }
                }
            }
        }
    }
}
