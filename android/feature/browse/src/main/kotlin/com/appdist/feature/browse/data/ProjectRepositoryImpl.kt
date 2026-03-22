package com.appdist.feature.browse.data

import com.appdist.core.network.ApiService
import com.appdist.core.network.dto.ProjectResponse
import com.appdist.feature.browse.domain.ProjectRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepositoryImpl @Inject constructor(
    private val api: ApiService
) : ProjectRepository {

    private val _projects = MutableStateFlow<List<ProjectResponse>>(emptyList())

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch { refresh() }
    }

    override fun getProjects(): Flow<List<ProjectResponse>> = _projects.asStateFlow()

    suspend fun refresh() {
        try {
            // workspaceId is not in scope for MVP — use a placeholder; server should return workspace builds
            // In a real app this would come from UserPreferences. For MVP use empty string.
            val response = api.getProjects("")
            if (response.isSuccessful) {
                _projects.value = response.body() ?: emptyList()
            }
        } catch (_: Exception) {}
    }
}
