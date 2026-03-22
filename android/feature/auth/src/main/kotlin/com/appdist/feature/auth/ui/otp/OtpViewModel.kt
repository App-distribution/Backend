package com.appdist.feature.auth.ui.otp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import com.appdist.feature.auth.domain.VerifyOtpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

@HiltViewModel
class OtpViewModel @Inject constructor(
    private val verifyOtp: VerifyOtpUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(OtpUiState())
    val state: StateFlow<OtpUiState> = _state.asStateFlow()

    private val _effects = Channel<OtpEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    var email: String = ""

    fun onAction(action: OtpAction) = when (action) {
        is OtpAction.OtpChanged -> _state.update {
            it.copy(otp = action.value.take(6), error = null)
        }
        OtpAction.VerifyClicked -> verify()
    }

    private fun verify() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val r = verifyOtp(email, _state.value.otp)) {
                is Result.Success -> _effects.send(OtpEffect.NavigateToHome)
                is Result.Error -> _state.update { it.copy(isLoading = false, error = r.error) }
            }
        }
    }
}
