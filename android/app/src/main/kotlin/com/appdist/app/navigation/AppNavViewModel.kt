package com.appdist.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdist.core.datastore.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface AuthState {
    data object Loading : AuthState
    data object Authenticated : AuthState
    data object Unauthenticated : AuthState
}

@HiltViewModel
class AppNavViewModel @Inject constructor(
    tokenManager: TokenManager
) : ViewModel() {

    val authState: StateFlow<AuthState> = tokenManager.isAuthenticated
        .map { if (it) AuthState.Authenticated else AuthState.Unauthenticated }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AuthState.Loading
        )
}
