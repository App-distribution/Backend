package com.appdist.feature.mine.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdist.core.database.dao.DownloadDao
import com.appdist.core.database.dao.InstallHistoryDao
import com.appdist.core.database.entity.DownloadEntity
import com.appdist.core.database.entity.InstallHistoryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class MineUiState(
    val installHistory: List<InstallHistoryEntity> = emptyList(),
    val activeDownloads: List<DownloadEntity> = emptyList()
)

@HiltViewModel
class MineViewModel @Inject constructor(
    installHistoryDao: InstallHistoryDao,
    downloadDao: DownloadDao
) : ViewModel() {

    val state: StateFlow<MineUiState> = combine(
        installHistoryDao.getAll(),
        downloadDao.getActiveDownloads()
    ) { history, downloads ->
        MineUiState(installHistory = history, activeDownloads = downloads)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MineUiState())
}
