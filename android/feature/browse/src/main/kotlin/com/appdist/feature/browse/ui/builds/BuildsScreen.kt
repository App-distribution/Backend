package com.appdist.feature.browse.ui.builds

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.appdist.core.common.AppError
import com.appdist.core.ui.components.EmptyScreen
import com.appdist.core.ui.components.ErrorScreen
import com.appdist.core.ui.components.LoadingScreen
import com.appdist.feature.browse.ui.builds.components.BuildCard
import com.appdist.feature.browse.ui.builds.components.FilterChipsRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildsScreen(
    projectId: String,
    onBuildClick: (String) -> Unit,
    onUploadClick: () -> Unit,
    viewModel: BuildsViewModel = hiltViewModel()
) {
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val builds = viewModel.builds.collectAsLazyPagingItems()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Сборки") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onUploadClick) {
                Icon(Icons.Default.Add, "Загрузить APK")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            var searchText by remember { mutableStateOf("") }
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it; viewModel.setSearch(it) },
                placeholder = { Text("Поиск по версии, ветке...") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            FilterChipsRow(
                filters = filters,
                onChannelSelected = viewModel::setChannel,
                onEnvironmentSelected = viewModel::setEnvironment,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(8.dp))

            when {
                builds.loadState.refresh is LoadState.Loading -> LoadingScreen()
                builds.loadState.refresh is LoadState.Error -> {
                    val e = (builds.loadState.refresh as LoadState.Error).error
                    ErrorScreen(
                        error = AppError.Unknown(e.message ?: "Unknown error"),
                        onRetry = { builds.refresh() }
                    )
                }
                builds.itemCount == 0 -> EmptyScreen("Сборок не найдено")
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(builds.itemCount) { index ->
                        builds[index]?.let { build ->
                            BuildCard(build = build, onClick = { onBuildClick(build.id) })
                        }
                    }
                    if (builds.loadState.append is LoadState.Loading) {
                        item {
                            CircularProgressIndicator(
                                modifier = Modifier.fillMaxWidth().wrapContentWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
