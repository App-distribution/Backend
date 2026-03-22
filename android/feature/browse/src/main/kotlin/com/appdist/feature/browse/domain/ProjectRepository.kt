package com.appdist.feature.browse.domain

import com.appdist.core.network.dto.ProjectResponse
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun getProjects(): Flow<List<ProjectResponse>>
}
