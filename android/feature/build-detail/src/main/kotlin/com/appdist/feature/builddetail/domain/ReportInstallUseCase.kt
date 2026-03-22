package com.appdist.feature.builddetail.domain

import com.appdist.core.common.Result
import javax.inject.Inject

class ReportInstallUseCase @Inject constructor(
    private val repository: BuildDetailRepository
) {
    suspend operator fun invoke(buildId: String): Result<Unit> = repository.reportInstall(buildId)
}
