package com.appdist.feature.builddetail.domain

import com.appdist.core.common.Result
import com.appdist.core.common.model.BuildUi
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBuildDetailUseCase @Inject constructor(
    private val repository: BuildDetailRepository
) {
    operator fun invoke(buildId: String): Flow<BuildUi> = repository.getBuildDetail(buildId)
    suspend fun getDownloadUrl(buildId: String): Result<String> = repository.getDownloadUrl(buildId)
}
