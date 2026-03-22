package com.appdist.feature.browse.ui.projects

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appdist.core.ui.components.LoadingScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    onProjectClick: (String) -> Unit,
    onUploadClick: () -> Unit,
    viewModel: ProjectsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Проекты") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onUploadClick) {
                Icon(Icons.Default.Add, "Загрузить APK")
            }
        }
    ) { padding ->
        if (state.isLoading) {
            LoadingScreen()
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(state.projects, key = { it.id }) { project ->
                    ElevatedCard(
                        onClick = { onProjectClick(project.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(project.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                project.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
