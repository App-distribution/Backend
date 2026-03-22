package com.appdist.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import com.appdist.feature.home.domain.model.AttentionItem
import com.appdist.feature.home.ui.components.AttentionItemCard
import com.appdist.feature.home.ui.components.BuildListItem

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
                    item { Text("Требует внимания", style = MaterialTheme.typography.titleMedium) }
                    items(state.attentionItems, key = { itemKey(it) }) { attentionItem ->
                        AttentionItemCard(attentionItem, onBuildClick)
                    }
                }
                if (state.recentBuilds.isNotEmpty()) {
                    item { Text("Последние сборки", style = MaterialTheme.typography.titleMedium) }
                    items(state.recentBuilds, key = { it.id }) { build ->
                        BuildListItem(build, onBuildClick)
                    }
                }
            }
        }
    }
}

private fun itemKey(item: AttentionItem): String = when (item) {
    is AttentionItem.MandatoryUpdate -> "mandatory_${item.build.id}"
    is AttentionItem.ExpiringBuild -> "expiring_${item.build.id}"
    is AttentionItem.NewBuildInSubscribedChannel -> "new_${item.build.id}"
}
