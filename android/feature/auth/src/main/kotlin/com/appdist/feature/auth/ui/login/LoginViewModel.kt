package com.appdist.feature.auth.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import com.appdist.feature.auth.domain.LoginUseCase
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
    val password: String = "",
    val isLoading: Boolean = false,
    val error: AppError? = null
)

sealed interface LoginAction {
    data class EmailChanged(val value: String) : LoginAction
    data class PasswordChanged(val value: String) : LoginAction
    data object Submit : LoginAction
}

sealed interface LoginEffect {
    data object NavigateToHome : LoginEffect
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _effects = Channel<LoginEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.EmailChanged -> _state.update { it.copy(email = action.value, error = null) }
            is LoginAction.PasswordChanged -> _state.update { it.copy(password = action.value, error = null) }
            LoginAction.Submit -> submit()
        }
    }

    private fun submit() {
        val email = _state.value.email.trim()
        val password = _state.value.password
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = loginUseCase(email, password)) {
                is Result.Success -> {
                    _state.update { it.copy(isLoading = false) }
                    _effects.send(LoginEffect.NavigateToHome)
                }
                is Result.Error -> _state.update { it.copy(isLoading = false, error = result.error) }
            }
        }
    }
}
