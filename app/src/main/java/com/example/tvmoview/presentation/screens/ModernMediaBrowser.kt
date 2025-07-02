package com.example.tvmoview.presentation.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tvmoview.MainActivity
import com.example.tvmoview.domain.model.*
import com.example.tvmoview.presentation.components.*
import com.example.tvmoview.presentation.viewmodels.MediaBrowserViewModel
import com.example.tvmoview.presentation.viewmodels.ViewMode
import com.example.tvmoview.presentation.viewmodels.SortBy
import com.example.tvmoview.presentation.viewmodels.SortOrder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

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
    val sortOrder by viewModel.sortOrder.collectAsState()
    val tileColumns by viewModel.tileColumns.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    val currentFolder by viewModel.currentFolderId.collectAsState()
    val lastIndex by viewModel.lastIndex.collectAsState()

    var showSortDialog by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = lastIndex)
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        onDispose { viewModel.saveScrollPosition(gridState.firstVisibleItemIndex) }
    }

    LaunchedEffect(lastIndex) {
        if (lastIndex > 0) gridState.scrollToItem(lastIndex)
    }

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
                sortOrder = sortOrder,
                tileColumns = tileColumns,
                onViewModeChange = { viewModel.toggleViewMode() },
                onTileColumnsChange = { viewModel.cycleTileColumns() },
                onSortClick = { showSortDialog = true },
                onOrderToggle = { viewModel.setSortOrder(if (sortOrder == SortOrder.ASC) SortOrder.DESC else SortOrder.ASC) },
                onRefreshClick = { viewModel.refresh() },
                onSettingsClick = onSettingsClick,
                onBackClick = onBackClick,
                isLoading = isLoading
            )

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    // 初回読み込み時のみローディング表示（データがない + 読み込み中）
                    isLoading && items.isEmpty() -> LoadingAnimation()

                    // データが空でローディング中でない場合
                    items.isEmpty() && !isLoading -> EmptyStateView()

                    // データがある場合は常にコンテンツ表示（手動更新中でも表示継続）
                    else -> {
                        when (viewMode) {
                            ViewMode.TILE -> {
                                ModernTileView(
                                    items = items,
                                    columnCount = tileColumns,
                                    state = gridState,
                                    onItemClick = { item ->
                                        if (item.isFolder) {
                                            Log.d("ModernMediaBrowser", "📂 フォルダ選択: ${item.name}")
                                            onFolderSelected(item.id)
                                        } else {
                                            Log.d("ModernMediaBrowser", "🎬 メディア選択: ${item.name}")
                                            Log.d("ModernMediaBrowser", "📊 downloadUrl: ${item.downloadUrl}")
                                            onMediaSelected(item)
                                        }
                                    }
                                )

                                // 撮影日・更新日順のときだけスクロールバーを表示
                                if ((sortBy == SortBy.SHOOT || sortBy == SortBy.DATE) && items.isNotEmpty()) {
                                    DateScrollIndicator(gridState, items)
                                }
                            }
                            ViewMode.LIST -> {
                                ModernListView(
                                    items = items,
                                    onItemClick = { item ->
                                        if (item.isFolder) {
                                            Log.d("ModernMediaBrowser", "📂 フォルダ選択: ${item.name}")
                                            onFolderSelected(item.id)
                                        } else {
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

            }
        }

        if (showSortDialog) {
            SortDialog(
                currentSort = sortBy,
                currentOrder = sortOrder,
                onSortSelected = { sort ->
                    viewModel.setSortBy(sort)
                    showSortDialog = false
                },
                onOrderSelected = { order -> viewModel.setSortOrder(order) },
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
    currentOrder: SortOrder,
    onSortSelected: (SortBy) -> Unit,
    onOrderSelected: (SortOrder) -> Unit,
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
                                SortBy.SHOOT -> "撮影日順"
                                SortBy.DATE -> "更新日時順"
                                SortBy.SIZE -> "サイズ順"
                                SortBy.NAME -> "名前順"
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

@Composable
private fun BoxScope.DateScrollIndicator(state: LazyGridState, items: List<MediaItem>) {
    val isScrolling by remember { derivedStateOf { state.isScrollInProgress } }
    val width = 56.dp
    val bar = 4.dp
    val alpha by animateFloatAsState(if (isScrolling) 1f else 0.6f, label = "a")
    val fontSize by animateFloatAsState(if (isScrolling) 20f else 12f, label = "fs")
    val dateWidth by animateDpAsState(if (isScrolling) 96.dp else 56.dp, label = "dw")
    val textStyle = MaterialTheme.typography.labelLarge.copy(fontSize = fontSize.sp)

    val currentDate by remember {
        derivedStateOf {
            if (state.firstVisibleItemIndex < items.size) {
                val item = items[state.firstVisibleItemIndex]
                SimpleDateFormat("yyyy/MM", Locale.getDefault()).format(item.lastModified)
            } else ""
        }
    }

    val progress = if (items.isNotEmpty()) {
        val itemSize = state.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 1
        val fullIndex = state.firstVisibleItemIndex +
            state.firstVisibleItemScrollOffset.toFloat() / itemSize
        fullIndex / (items.size - 1).coerceAtLeast(1)
    } else 0f
    val viewport = with(LocalDensity.current) { state.layoutInfo.viewportSize.height.toDp() }
    val dateHeight = 32.dp
    val dateOffset = (viewport - dateHeight).coerceAtLeast(0.dp) * progress

    Box(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .fillMaxHeight()
            .width(width)
            .padding(vertical = 48.dp, horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(bar)
                .fillMaxHeight()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(2.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .width(bar)
                    .fillMaxHeight(progress)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }

        if (currentDate.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-dateWidth), y = dateOffset)
                    .width(dateWidth)
                    .alpha(alpha)
            ) {
                Text(
                    text = currentDate,
                    style = textStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}
