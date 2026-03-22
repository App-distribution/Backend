package com.appdist.feature.home.domain

import com.appdist.feature.home.domain.model.AttentionItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class GetAttentionItemsUseCase @Inject constructor(
    private val repository: HomeRepository
) {
    operator fun invoke(): Flow<List<AttentionItem>> =
        repository.getRecentBuilds(50).map { builds ->
            val items = mutableListOf<AttentionItem>()
            val nowMs = System.currentTimeMillis()
            val threeDaysMs = TimeUnit.DAYS.toMillis(3)

            builds.filter { it.status == "mandatory" }
                .forEach { items.add(AttentionItem.MandatoryUpdate(it)) }

            builds.filter { build ->
                val exp = build.expiryDate
                exp != null && exp > nowMs && exp - nowMs <= threeDaysMs
            }.forEach { build ->
                val exp = build.expiryDate!!
                val daysLeft = TimeUnit.MILLISECONDS.toDays(exp - nowMs).toInt()
                items.add(AttentionItem.ExpiringBuild(build, daysLeft))
            }

            items
        }
}
