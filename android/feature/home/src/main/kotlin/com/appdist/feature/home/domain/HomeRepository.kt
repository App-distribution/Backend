package com.appdist.feature.home.domain

import com.appdist.core.common.model.BuildUi
import kotlinx.coroutines.flow.Flow

interface HomeRepository {
    fun getRecentBuilds(limit: Int = 20): Flow<List<BuildUi>>
}
