package com.appdist.feature.builddetail

import androidx.lifecycle.SavedStateHandle
import androidx.work.WorkManager
import app.cash.turbine.test
import com.appdist.core.common.AppError
import com.appdist.core.common.Result
import com.appdist.core.common.model.BuildUi
import com.appdist.core.common.model.InstallStatus
import com.appdist.feature.builddetail.domain.*
import com.appdist.feature.builddetail.ui.BuildDetailAction
import com.appdist.feature.builddetail.ui.BuildDetailViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BuildDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var getBuildDetail: GetBuildDetailUseCase
    private lateinit var getInstallStatus: GetInstallStatusUseCase
    private lateinit var downloadBuild: DownloadBuildUseCase
    private lateinit var reportInstall: ReportInstallUseCase
    private lateinit var workManager: WorkManager
    private lateinit var context: android.content.Context
    private lateinit var viewModel: BuildDetailViewModel

    private val fakeBuild = BuildUi(
        id = "b1", projectId = "p1", projectName = "Test", packageName = "com.test",
        versionName = "1.0", versionCode = 1L, channel = "alpha", environment = "dev",
        buildType = "debug", changelog = null, fileSize = 1024L,
        checksumSha256 = "abc", status = "active", isLatestInChannel = true,
        uploadDate = 0L, uploaderName = "dev", minSdk = 24, targetSdk = 35,
        certFingerprint = null, abis = emptyList(), expiryDate = null,
        branch = null, commitHash = null
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getBuildDetail = mockk()
        getInstallStatus = mockk()
        downloadBuild = mockk()
        reportInstall = mockk(relaxed = true)
        workManager = mockk(relaxed = true)
        context = mockk(relaxed = true)
        every { context.packageName } returns "com.appdist.app"

        every { getBuildDetail(any<String>()) } returns flowOf(fakeBuild)
        every { getInstallStatus(any(), any(), any(), any()) } returns InstallStatus.NotInstalled

        val savedState = SavedStateHandle(mapOf("buildId" to "b1"))
        viewModel = BuildDetailViewModel(savedState, context, getBuildDetail, getInstallStatus, downloadBuild, reportInstall, workManager)
    }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `loads build on init`() = runTest {
        viewModel.state.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNotNull(state.build)
            assertEquals("b1", state.build?.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `copy link emits CopyToClipboard effect`() = runTest {
        viewModel.effects.test {
            viewModel.onAction(BuildDetailAction.CopyLinkClicked)
            val effect = awaitItem()
            assertTrue(effect is com.appdist.feature.builddetail.ui.BuildDetailEffect.CopyToClipboard)
            assertEquals("appdist://builds/b1", (effect as com.appdist.feature.builddetail.ui.BuildDetailEffect.CopyToClipboard).text)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `error state set when build load fails`() = runTest {
        every { getBuildDetail(any<String>()) } returns flow { throw RuntimeException("Network error") }
        val savedState = SavedStateHandle(mapOf("buildId" to "b1"))
        val vm = BuildDetailViewModel(savedState, context, getBuildDetail, getInstallStatus, downloadBuild, reportInstall, workManager)
        vm.state.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertNotNull(state.error)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
