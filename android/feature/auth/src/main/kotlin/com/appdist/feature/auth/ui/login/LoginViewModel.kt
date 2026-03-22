package com.appdist.feature.auth.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import com.appdist.feature.auth.domain.RequestOtpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val error: AppError? = null
)

sealed interface LoginAction {
    data class EmailChanged(val value: String) : LoginAction
    data object SubmitClicked : LoginAction
}

sealed interface LoginEffect {
    data class NavigateToOtp(val email: String) : LoginEffect
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val requestOtp: RequestOtpUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _effects = Channel<LoginEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.EmailChanged -> _state.update { it.copy(email = action.value, error = null) }
            LoginAction.SubmitClicked -> submit()
        }
    }

    private fun submit() {
        val email = _state.value.email.trim()
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = requestOtp(email)) {
                is Result.Success -> _effects.send(LoginEffect.NavigateToOtp(email))
                is Result.Error -> _state.update { it.copy(isLoading = false, error = result.error) }
            }
        }
    }
}
