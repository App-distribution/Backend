package com.appdist.feature.home.domain

import com.appdist.core.common.model.BuildUi
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRecentBuildsUseCase @Inject constructor(
    private val repository: HomeRepository
) {
    operator fun invoke(limit: Int = 10): Flow<List<BuildUi>> =
        repository.getRecentBuilds(limit)
}
