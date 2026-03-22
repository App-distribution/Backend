package com.appdist.feature.mine

import app.cash.turbine.test
import com.appdist.core.database.dao.DownloadDao
import com.appdist.core.database.dao.InstallHistoryDao
import com.appdist.core.database.entity.DownloadEntity
import com.appdist.core.database.entity.InstallHistoryEntity
import com.appdist.feature.mine.ui.MineViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MineViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var installHistoryDao: InstallHistoryDao
    private lateinit var downloadDao: DownloadDao
    private lateinit var viewModel: MineViewModel

    private val fakeHistory = listOf(
        InstallHistoryEntity("id1", "b1", "com.test", "1.0", 1L, System.currentTimeMillis())
    )
    private val fakeDownloads = listOf(
        DownloadEntity("b2", null, "downloading", 0.5f, 512L, 1024L, System.currentTimeMillis())
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        installHistoryDao = mockk()
        downloadDao = mockk()
        every { installHistoryDao.getAll() } returns flowOf(fakeHistory)
        every { downloadDao.getActiveDownloads() } returns flowOf(fakeDownloads)
        viewModel = MineViewModel(installHistoryDao, downloadDao)
    }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `combines install history and active downloads`() = runTest {
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(1, state.installHistory.size)
            assertEquals("com.test", state.installHistory[0].packageName)
            assertEquals(1, state.activeDownloads.size)
            assertEquals("b2", state.activeDownloads[0].buildId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty state when no data`() = runTest {
        every { installHistoryDao.getAll() } returns flowOf(emptyList())
        every { downloadDao.getActiveDownloads() } returns flowOf(emptyList())
        val vm = MineViewModel(installHistoryDao, downloadDao)
        vm.state.test {
            val state = awaitItem()
            assertTrue(state.installHistory.isEmpty())
            assertTrue(state.activeDownloads.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `state updates when download completes and disappears`() = runTest {
        val historyFlow = MutableStateFlow(fakeHistory)
        val downloadsFlow = MutableStateFlow(fakeDownloads)
        every { installHistoryDao.getAll() } returns historyFlow
        every { downloadDao.getActiveDownloads() } returns downloadsFlow
        val vm = MineViewModel(installHistoryDao, downloadDao)
        vm.state.test {
            val first = awaitItem()
            assertEquals(1, first.activeDownloads.size)
            // Download completes — active list becomes empty
            downloadsFlow.value = emptyList()
            val second = awaitItem()
            assertTrue(second.activeDownloads.isEmpty())
            assertEquals(1, second.installHistory.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
