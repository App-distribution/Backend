package com.appdist.feature.browse.domain

import com.appdist.feature.browse.ui.builds.BuildFilters
import javax.inject.Inject

class GetBuildsUseCase @Inject constructor(private val repo: BuildRepository) {
    operator fun invoke(projectId: String, filters: BuildFilters) =
        repo.getBuilds(projectId, filters)
}
