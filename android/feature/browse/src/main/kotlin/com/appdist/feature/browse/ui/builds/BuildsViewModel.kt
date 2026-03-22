package com.appdist.feature.browse.ui.builds

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.appdist.feature.browse.domain.GetBuildsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class BuildsViewModel @Inject constructor(
    private val getBuilds: GetBuildsUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val projectId: String = checkNotNull(savedStateHandle["projectId"])

    private val _filters = MutableStateFlow(BuildFilters())
    val filters: StateFlow<BuildFilters> = _filters.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val builds = _filters.flatMapLatest { filters ->
        getBuilds(projectId, filters)
    }.cachedIn(viewModelScope)

    fun setChannel(channel: String?) = _filters.update { it.copy(channel = channel) }
    fun setEnvironment(env: String?) = _filters.update { it.copy(environment = env) }
    fun setSearch(query: String) = _filters.update { it.copy(searchQuery = query) }
    fun clearFilters() = _filters.update { BuildFilters() }
}
