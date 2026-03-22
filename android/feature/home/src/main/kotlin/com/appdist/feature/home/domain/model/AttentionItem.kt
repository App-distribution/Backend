package com.appdist.feature.home.domain.model

import com.appdist.core.common.model.BuildUi

sealed interface AttentionItem {
    data class MandatoryUpdate(val build: BuildUi) : AttentionItem
    data class ExpiringBuild(val build: BuildUi, val daysLeft: Int) : AttentionItem
    data class NewBuildInSubscribedChannel(val build: BuildUi) : AttentionItem
}
