# PowerShell用スクリプト (create_components.ps1)
# 使用方法: 最初のスクリプト実行後にこれを実行

$basePath = "app\src\main\java\com\example\tvmoview\presentation"

# ModernMediaBrowser.kt
$modernMediaBrowser = @'
package com.example.tvmoview.presentation.screens

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
import com.example.tvmoview.presentation.viewmodels.MediaBrowserViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernMediaBrowser(
    folderId: String? = null,
    onMediaSelected: (MediaItem) -> Unit,
    onFolderSelected: (String) -> Unit,
    onSettingsClick: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null
) {
    val viewModel: MediaBrowserViewModel = viewModel(
        factory = MediaBrowserViewModelFactory(MainActivity.mediaRepository)
    )

    val items by viewModel.items.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()

    var showSortDialog by remember { mutableStateOf(false) }

    LaunchedEffect(folderId) {
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
                        AnimatedContent(
                            targetState = viewMode,
                            transitionSpec = { fadeIn() with fadeOut() },
                            label = "ViewModeTransition"
                        ) { mode ->
                            when (mode) {
                                ViewMode.TILE -> {
                                    ModernTileView(
                                        items = items,
                                        onItemClick = { item ->
                                            if (item.isFolder) {
                                                onFolderSelected(item.id)
                                            } else {
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
                                                onFolderSelected(item.id)
                                            } else {
                                                onMediaSelected(item)
                                            }
                                        }
                                    )
                                }
                            }
                        }
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
'@

# コンポーネントファイル用のハッシュテーブル
$componentFiles = @{
    "$basePath\screens\ModernMediaBrowser.kt" = $modernMediaBrowser

    "$basePath\components\ModernTopBar.kt" = @'
package com.example.tvmoview.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tvmoview.domain.model.ViewMode

@Composable
fun ModernTopBar(
    currentPath: String,
    viewMode: ViewMode,
    onViewModeChange: () -> Unit,
    onSortClick: () -> Unit,
    onSettingsClick: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBackClick != null) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "戻る")
                    }
                }
                Text(
                    text = if (currentPath.isEmpty()) "OneDrive TV Viewer" else currentPath,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Row {
                IconButton(onClick = onViewModeChange) {
                    Icon(
                        imageVector = if (viewMode == ViewMode.TILE) Icons.Default.List else Icons.Default.GridView,
                        contentDescription = "表示モード切替"
                    )
                }
                IconButton(onClick = onSortClick) {
                    Icon(Icons.Default.Sort, contentDescription = "ソート")
                }
                if (onSettingsClick != null) {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "設定")
                    }
                }
            }
        }
    }
}
'@

    "$basePath\components\ModernTileView.kt" = @'
package com.example.tvmoview.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tvmoview.domain.model.MediaItem

@Composable
fun ModernTileView(
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 200.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(items) { item ->
            ModernMediaCard(
                item = item,
                onClick = { onItemClick(item) }
            )
        }
    }
}
'@

    "$basePath\components\ModernListView.kt" = @'
package com.example.tvmoview.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tvmoview.domain.model.MediaItem

@Composable
fun ModernListView(
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { item ->
            ModernListItem(
                item = item,
                onClick = { onItemClick(item) }
            )
        }
    }
}
'@

    "$basePath\components\LoadingAnimation.kt" = @'
package com.example.tvmoview.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoadingAnimation() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "読み込み中...",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
'@

    "$basePath\components\EmptyStateView.kt" = @'
package com.example.tvmoview.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EmptyStateView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "フォルダが空です",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
'@

    "$basePath\components\ModernMediaCard.kt" = @'
package com.example.tvmoview.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.tvmoview.domain.model.MediaItem
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ModernMediaCard(
    item: MediaItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        item.isFolder -> Icons.Default.Folder
                        item.isVideo -> Icons.Default.PlayArrow
                        item.isImage -> Icons.Default.Image
                        else -> Icons.Default.Description
                    },
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (!item.isFolder) {
                    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                    Text(
                        text = "${formatFileSize(item.size)} • ${dateFormat.format(item.lastModified)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatFileSize(size: Long): String {
    val kb = 1024
    val mb = kb * 1024
    val gb = mb * 1024
    
    return when {
        size >= gb -> String.format("%.1f GB", size.toDouble() / gb)
        size >= mb -> String.format("%.1f MB", size.toDouble() / mb)
        size >= kb -> String.format("%.1f KB", size.toDouble() / kb)
        else -> "$size B"
    }
}
'@

    "$basePath\components\ModernListItem.kt" = @'
package com.example.tvmoview.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.tvmoview.domain.model.MediaItem
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ModernListItem(
    item: MediaItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    item.isFolder -> Icons.Default.Folder
                    item.isVideo -> Icons.Default.PlayArrow
                    item.isImage -> Icons.Default.Image
                    else -> Icons.Default.Description
                },
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (!item.isFolder) {
                    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                    Text(
                        text = "${formatFileSize(item.size)} • ${dateFormat.format(item.lastModified)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatFileSize(size: Long): String {
    val kb = 1024
    val mb = kb * 1024
    val gb = mb * 1024
    
    return when {
        size >= gb -> String.format("%.1f GB", size.toDouble() / gb)
        size >= mb -> String.format("%.1f MB", size.toDouble() / mb)
        size >= kb -> String.format("%.1f KB", size.toDouble() / kb)
        else -> "$size B"
    }
}
'@

    "$basePath\components\SortDialog.kt" = @'
package com.example.tvmoview.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.tvmoview.domain.model.SortBy

@Composable
fun SortDialog(
    currentSort: SortBy,
    onSortSelected: (SortBy) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "並び順",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                val sortOptions = listOf(
                    SortBy.NAME to "名前",
                    SortBy.DATE_MODIFIED to "更新日時",
                    SortBy.DATE_TAKEN to "撮影日時",
                    SortBy.SIZE to "サイズ"
                )
                
                sortOptions.forEach { (sortBy, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = currentSort == sortBy,
                                onClick = { onSortSelected(sortBy) }
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSort == sortBy,
                            onClick = { onSortSelected(sortBy) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = label)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("キャンセル")
                    }
                }
            }
        }
    }
}
'@
}

# コンポーネントファイルを作成
foreach ($file in $componentFiles.GetEnumerator()) {
    $file.Value | Out-File -FilePath $file.Key -Encoding UTF8
    Write-Host "ファイル作成: $($file.Key)"
}

Write-Host "`n✅ 全てのファイルの作成が完了しました！"
Write-Host "`n📋 次のステップ:"
Write-Host "1. build.gradle (Module: app) に依存関係を追加"
Write-Host "2. AndroidManifest.xml を更新"
Write-Host "3. プロジェクトをビルド"

# build.gradle の内容を表示
Write-Host "`n📄 build.gradle (Module: app) に追加する依存関係:"
Write-Host @'

dependencies {
    implementation "androidx.core:core-ktx:1.12.0"
    implementation "androidx.lifecycle:lifecycle-runtime-ktx:2.7.0"
    implementation "androidx.activity:activity-compose:1.8.2"
    
    // Compose BOM
    implementation platform("androidx.compose:compose-bom:2024.02.00")
    implementation "androidx.compose.ui:ui"
    implementation "androidx.compose.ui:ui-graphics"
    implementation "androidx.compose.ui:ui-tooling-preview"
    implementation "androidx.compose.material3:material3"
    
    // Navigation
    implementation "androidx.navigation:navigation-compose:2.7.6"
    
    // ViewModel
    implementation "androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0"
    
    // Coroutines
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"
    
    // TV用依存関係
    implementation "androidx.tv:tv-foundation:1.0.0-alpha10"
    implementation "androidx.tv:tv-material:1.0.0-alpha10"
    implementation "androidx.leanback:leanback:1.0.0"
    
    debugImplementation "androidx.compose.ui:ui-tooling"
    debugImplementation "androidx.compose.ui:ui-test-manifest"
}
'@