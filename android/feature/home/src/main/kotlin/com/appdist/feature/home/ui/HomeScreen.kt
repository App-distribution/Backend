package com.appdist.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appdist.core.ui.components.EmptyScreen
import com.appdist.core.ui.components.LoadingScreen
import com.appdist.feature.home.ui.components.AttentionSection
import com.appdist.feature.home.ui.components.RecentBuildsSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onBuildClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(topBar = {
        TopAppBar(title = { Text("AppDistribution") })
    }) { padding ->
        when {
            state.isLoading -> LoadingScreen(Modifier.padding(padding))
            state.attentionItems.isEmpty() && state.recentBuilds.isEmpty() ->
                EmptyScreen("Нет доступных сборок", Modifier.padding(padding))
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.attentionItems.isNotEmpty()) {
                    item { AttentionSection(state.attentionItems, onBuildClick) }
                }
                if (state.recentBuilds.isNotEmpty()) {
                    item { RecentBuildsSection(state.recentBuilds, onBuildClick) }
                }
            }
        }
    }
}
