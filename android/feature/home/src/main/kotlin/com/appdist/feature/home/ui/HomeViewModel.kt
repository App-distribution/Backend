package com.appdist.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdist.core.common.model.BuildUi
import com.appdist.feature.home.domain.GetAttentionItemsUseCase
import com.appdist.feature.home.domain.GetRecentBuildsUseCase
import com.appdist.feature.home.domain.model.AttentionItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val attentionItems: List<AttentionItem> = emptyList(),
    val recentBuilds: List<BuildUi> = emptyList(),
    val isLoading: Boolean = true
)

sealed interface HomeAction {
    data object Refresh : HomeAction
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getAttentionItems: GetAttentionItemsUseCase,
    private val getRecentBuilds: GetRecentBuildsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init { load() }

    fun onAction(action: HomeAction) = when (action) {
        HomeAction.Refresh -> load()
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            combine(
                getAttentionItems(),
                getRecentBuilds()
            ) { attention, recent ->
                HomeUiState(attentionItems = attention, recentBuilds = recent, isLoading = false)
            }.catch { _state.update { it.copy(isLoading = false) } }
             .collect { _state.value = it }
        }
    }
}
