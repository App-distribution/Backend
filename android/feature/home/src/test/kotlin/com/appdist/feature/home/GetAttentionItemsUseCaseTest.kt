package com.appdist.feature.home

import app.cash.turbine.test
import com.appdist.core.common.model.BuildUi
import com.appdist.core.common.model.InstallStatus
import com.appdist.feature.home.domain.GetAttentionItemsUseCase
import com.appdist.feature.home.domain.HomeRepository
import com.appdist.feature.home.domain.model.AttentionItem
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GetAttentionItemsUseCaseTest {

    private val repository = mockk<HomeRepository>()
    private val useCase = GetAttentionItemsUseCase(repository)

    @Test
    fun `mandatory build appears as MandatoryUpdate`() = runTest {
        val mandatory = buildUi(status = "mandatory")
        every { repository.getRecentBuilds(any()) } returns flowOf(listOf(mandatory))

        useCase().test {
            val items = awaitItem()
            assertTrue(items.any { it is AttentionItem.MandatoryUpdate })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `expiring build within 3 days appears as ExpiringBuild`() = runTest {
        val expiringIn2Days = buildUi(expiryDate = System.currentTimeMillis() + 2 * 24 * 60 * 60 * 1000L)
        every { repository.getRecentBuilds(any()) } returns flowOf(listOf(expiringIn2Days))

        useCase().test {
            val items = awaitItem()
            assertTrue(items.any { it is AttentionItem.ExpiringBuild })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `normal build does not appear in attention`() = runTest {
        val normal = buildUi(status = "active", expiryDate = null)
        every { repository.getRecentBuilds(any()) } returns flowOf(listOf(normal))

        useCase().test {
            val items = awaitItem()
            assertTrue(items.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun buildUi(status: String = "active", expiryDate: Long? = null) = BuildUi(
        id = "b1", projectId = "p1", projectName = "Test", packageName = "com.test",
        versionName = "1.0.0", versionCode = 1, channel = "alpha", environment = "qa",
        buildType = "debug", changelog = null, fileSize = 1024, checksumSha256 = "abc",
        status = status, isLatestInChannel = false, uploadDate = System.currentTimeMillis(),
        uploaderName = "dev", minSdk = 24, targetSdk = 35, certFingerprint = null,
        abis = emptyList(), expiryDate = expiryDate, branch = null, commitHash = null
    )
}
