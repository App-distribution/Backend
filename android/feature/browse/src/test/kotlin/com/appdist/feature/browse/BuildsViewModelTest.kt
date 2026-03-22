package com.appdist.feature.browse

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import app.cash.turbine.test
import com.appdist.feature.browse.domain.BuildRepository
import com.appdist.feature.browse.domain.GetBuildsUseCase
import com.appdist.feature.browse.ui.builds.BuildFilters
import com.appdist.feature.browse.ui.builds.BuildsViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BuildsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: BuildRepository
    private lateinit var viewModel: BuildsViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        every { repository.getBuilds(any(), any()) } returns flowOf(PagingData.empty())
        val useCase = GetBuildsUseCase(repository)
        val savedState = SavedStateHandle(mapOf("projectId" to "proj-1"))
        viewModel = BuildsViewModel(useCase, savedState)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial filters are empty`() = runTest {
        viewModel.filters.test {
            val filters = awaitItem()
            assertNull(filters.channel)
            assertNull(filters.environment)
            assertEquals("", filters.searchQuery)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setChannel updates filter`() = runTest {
        viewModel.filters.test {
            awaitItem() // initial
            viewModel.setChannel("alpha")
            assertEquals("alpha", awaitItem().channel)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearFilters resets to defaults`() = runTest {
        viewModel.setChannel("beta")
        viewModel.setEnvironment("qa")
        viewModel.clearFilters()
        viewModel.filters.test {
            val filters = awaitItem()
            assertTrue(filters.isEmpty)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
