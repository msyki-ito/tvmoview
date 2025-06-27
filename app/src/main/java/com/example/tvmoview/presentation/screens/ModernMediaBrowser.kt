package com.example.tvmoview.presentation.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tvmoview.MainActivity
import com.example.tvmoview.domain.model.*
import com.example.tvmoview.presentation.components.*
import com.example.tvmoview.presentation.viewmodels.MediaBrowserViewModel
import com.example.tvmoview.presentation.viewmodels.ViewMode
import com.example.tvmoview.presentation.viewmodels.SortBy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernMediaBrowser(
    folderId: String? = null,
    onMediaSelected: (MediaItem) -> Unit,
    onFolderSelected: (String) -> Unit,
    onSettingsClick: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null
) {
    val viewModel: MediaBrowserViewModel = viewModel()
    val items by viewModel.items.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()

    var showSortDialog by remember { mutableStateOf(false) }

    // OneDrive統合：データ取得処理
    LaunchedEffect(folderId) {
        Log.d("ModernMediaBrowser", "📁 フォルダ読み込み開始: $folderId")
        viewModel.loadItems(folderId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column {
            ModernTopBar(
                currentPath = currentPath,
                viewMode = viewMode,
                onViewModeChange = { viewModel.toggleViewMode() },
                onSortClick = { showSortDialog = true },
                onSettingsClick = onSettingsClick,
                onBackClick = onBackClick
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> LoadingAnimation()
                    items.isEmpty() -> EmptyStateView()
                    else -> {
                        when (viewMode) {
                            ViewMode.TILE -> {
                                ModernTileView(
                                    items = items,
                                    onItemClick = { item ->
                                        if (item.isFolder) {
                                            Log.d("ModernMediaBrowser", "📂 フォルダ選択: ${item.name}")
                                            onFolderSelected(item.id)
                                        } else {
                                            // OneDrive統合：downloadURL付きでMediaItemを渡す
                                            Log.d("ModernMediaBrowser", "🎬 メディア選択: ${item.name}")
                                            Log.d("ModernMediaBrowser", "📊 downloadUrl: ${item.downloadUrl}")
                                            onMediaSelected(item)
                                        }
                                    }
                                )
                            }
                            ViewMode.LIST -> {
                                ModernListView(
                                    items = items,
                                    onItemClick = { item ->
                                        if (item.isFolder) {
                                            Log.d("ModernMediaBrowser", "📂 フォルダ選択: ${item.name}")
                                            onFolderSelected(item.id)
                                        } else {
                                            // OneDrive統合：downloadURL付きでMediaItemを渡す
                                            Log.d("ModernMediaBrowser", "🎬 メディア選択: ${item.name}")
                                            Log.d("ModernMediaBrowser", "📊 downloadUrl: ${item.downloadUrl}")
                                            onMediaSelected(item)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // OneDrive統合状態表示（デバッグ用）
                if (MainActivity.authManager.isAuthenticated()) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    ) {
                        Text(
                            text = "🔐 OneDrive接続中",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }

        if (showSortDialog) {
            SortDialog(
                currentSort = sortBy,
                onSortSelected = { sort ->
                    viewModel.setSortBy(sort)
                    showSortDialog = false
                },
                onDismiss = { showSortDialog = false }
            )
        }
    }
}

@Composable
fun LoadingAnimation() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("OneDriveから読み込み中...")
        }
    }
}

@Composable
fun EmptyStateView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "フォルダが空です",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "メディアファイルがありません",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SortDialog(
    currentSort: SortBy,
    onSortSelected: (SortBy) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("並び順") },
        text = {
            Column {
                SortBy.values().forEach { sortOption ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSort == sortOption,
                            onClick = { onSortSelected(sortOption) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (sortOption) {
                                SortBy.NAME -> "名前順"
                                SortBy.DATE -> "更新日時順"
                                SortBy.SIZE -> "サイズ順"
                                SortBy.TYPE -> "種類順"
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる")
            }
        }
    )
}