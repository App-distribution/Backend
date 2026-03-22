package com.appdist.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdist.core.datastore.TokenManager
import com.appdist.core.datastore.UserPreferencesStore
import com.appdist.core.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SettingsUiEffect {
    data object NavigateToLogin : SettingsUiEffect
    data class ShowError(val message: String) : SettingsUiEffect
}

data class SettingsUiState(
    val email: String = "",
    val role: String = "",
    val notificationsEnabled: Boolean = true,
    val isLoading: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsStore: UserPreferencesStore,
    private val tokenManager: TokenManager,
    private val api: ApiService
) : ViewModel() {

    private val _effect = Channel<SettingsUiEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    val state: StateFlow<SettingsUiState> = combine(
        prefsStore.notificationsEnabled,
        flow { emit(loadUserInfo()) }
    ) { notifEnabled, userInfo ->
        userInfo.copy(notificationsEnabled = notifEnabled)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    private suspend fun loadUserInfo(): SettingsUiState {
        return try {
            val response = api.getMe()
            if (response.isSuccessful) {
                val user = response.body()
                SettingsUiState(email = user?.email ?: "", role = user?.role ?: "")
            } else SettingsUiState()
        } catch (e: Exception) {
            SettingsUiState()
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsStore.setNotificationsEnabled(enabled) }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                tokenManager.clear()
                _effect.send(SettingsUiEffect.NavigateToLogin)
            } catch (e: Exception) {
                _effect.send(SettingsUiEffect.ShowError("Ошибка выхода"))
            }
        }
    }
}
