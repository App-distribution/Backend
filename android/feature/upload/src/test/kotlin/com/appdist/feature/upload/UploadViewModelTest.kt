package com.appdist.feature.upload

import android.content.Context
import app.cash.turbine.test
import com.appdist.feature.upload.domain.ExtractApkMetadataUseCase
import com.appdist.feature.upload.domain.UploadBuildUseCase
import com.appdist.feature.upload.ui.UploadAction
import com.appdist.feature.upload.ui.UploadViewModel
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UploadViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var extractMetadata: ExtractApkMetadataUseCase
    private lateinit var uploadBuild: UploadBuildUseCase
    private lateinit var viewModel: UploadViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        extractMetadata = mockk()
        uploadBuild = mockk()
        viewModel = UploadViewModel(context, extractMetadata, uploadBuild)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty`() = runTest {
        viewModel.state.test {
            val state = awaitItem()
            assertNull(state.selectedApkUri)
            assertNull(state.extractedMetadata)
            assertEquals("internal", state.channel)
            assertEquals("", state.changelog)
            assertNull(state.uploadProgress)
            assertFalse(state.isSuccess)
            assertNull(state.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `channel change updates state`() = runTest {
        viewModel.state.test {
            awaitItem() // initial
            viewModel.onAction(UploadAction.ChannelChanged("alpha"))
            assertEquals("alpha", awaitItem().channel)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `changelog change updates state`() = runTest {
        viewModel.state.test {
            awaitItem() // initial
            viewModel.onAction(UploadAction.ChangelogChanged("Fixed crash on startup"))
            assertEquals("Fixed crash on startup", awaitItem().changelog)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
