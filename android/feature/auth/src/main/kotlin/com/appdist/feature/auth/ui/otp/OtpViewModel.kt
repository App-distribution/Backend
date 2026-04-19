package com.appdist.feature.auth.ui.otp

import androidx.lifecycle.ViewModel
import com.appdist.core.common.AppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

data class OtpUiState(
    val otp: String = "",
    val isLoading: Boolean = false,
    val error: AppError? = null
)

sealed interface OtpAction {
    data class OtpChanged(val value: String) : OtpAction
    data object VerifyClicked : OtpAction
}

sealed interface OtpEffect {
    data object NavigateToHome : OtpEffect
}

// TODO: OTP flow is being replaced with email+password auth — this ViewModel is a stub
@HiltViewModel
class OtpViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(OtpUiState())
    val state: StateFlow<OtpUiState> = _state.asStateFlow()

    private val _effects = Channel<OtpEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    var email: String = ""

    fun onAction(action: OtpAction) = Unit
}
