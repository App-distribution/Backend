package com.appdist.feature.browse.domain

import javax.inject.Inject

class GetProjectsUseCase @Inject constructor(private val repo: ProjectRepository) {
    operator fun invoke() = repo.getProjects()
}
