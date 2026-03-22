package com.appdist.feature.browse.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdist.core.network.dto.ProjectResponse
import com.appdist.feature.browse.domain.GetProjectsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class ProjectsUiState(
    val projects: List<ProjectResponse> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val getProjects: GetProjectsUseCase
) : ViewModel() {

    val state: StateFlow<ProjectsUiState> = getProjects()
        .map { projects -> ProjectsUiState(projects = projects, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProjectsUiState()
        )
}
