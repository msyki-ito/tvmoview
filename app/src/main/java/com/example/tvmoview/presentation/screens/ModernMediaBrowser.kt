﻿package com.example.tvmoview.presentation.screens

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import com.example.tvmoview.MainActivity
import com.example.tvmoview.R
import com.example.tvmoview.domain.model.*
import com.example.tvmoview.presentation.components.*
import com.example.tvmoview.presentation.viewmodels.MediaBrowserViewModel
import com.example.tvmoview.presentation.viewmodels.ViewMode
import com.example.tvmoview.presentation.viewmodels.SortBy
import com.example.tvmoview.presentation.viewmodels.SortOrder
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernMediaBrowser(
    viewModel: MediaBrowserViewModel,
    folderId: String? = null,
    onMediaSelected: (MediaItem) -> Unit,
    onFolderSelected: (String) -> Unit,
    onSettingsClick: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null
) {
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
    var showTopBar by remember { mutableStateOf(true) }
    var previousIndex by remember { mutableStateOf(0) }
    var previousOffset by remember { mutableStateOf(0) }
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { (index, offset) ->
                when {
                    index > previousIndex || (index == previousIndex && offset > previousOffset) -> showTopBar = false
                    index < previousIndex || (index == previousIndex && offset < previousOffset) -> showTopBar = true
                }
                previousIndex = index
                previousOffset = offset
            }
    }

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
        modifier = Modifier.fillMaxSize()
    ) {
        Column {
            AnimatedVisibility(
                visible = showTopBar,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(150))
            ) {
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
            }

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

                                // 細いシークバー（撮影日・更新日順）
                                if ((sortBy == SortBy.SHOOT || sortBy == SortBy.DATE) && items.isNotEmpty()) {
                                    // 現在表示中アイテムの日付を監視
                                    val currentVisibleDate by remember {
                                        derivedStateOf {
                                            if (gridState.firstVisibleItemIndex < items.size) {
                                                val item = items[gridState.firstVisibleItemIndex]
                                                SimpleDateFormat("yyyy/MM", Locale.getDefault()).format(item.lastModified)
                                            } else ""
                                        }
                                    }

                                    // 細いシークバー
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .fillMaxHeight()
                                            .width(24.dp)  // 80dp → 24dpに縮小
                                            .padding(vertical = 48.dp, horizontal = 4.dp)
                                    ) {
                                        val progress = if (items.isNotEmpty()) {
                                            gridState.firstVisibleItemIndex.toFloat() / (items.size - 1).coerceAtLeast(1).toFloat()
                                        } else 0f
                                        val viewport = with(LocalDensity.current) { gridState.layoutInfo.viewportSize.height.toDp() }
                                        val dateOffset = viewport * progress - 24.dp

                                        // 背景バー（細い）
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .width(4.dp)  // 40dp → 4dpに細く
                                                .fillMaxHeight()
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                    shape = RoundedCornerShape(2.dp)
                                                )
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .width(4.dp)
                                                    .fillMaxHeight(progress)
                                                    .background(
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                                        shape = RoundedCornerShape(2.dp)
                                                    )
                                            )
                                        }

                                        // 日付表示（上部）
                                        if (currentVisibleDate.isNotEmpty()) {
                                            Text(
                                                text = currentVisibleDate,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Clip,
                                                softWrap = false,
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .offset(x = (-72).dp, y = dateOffset)
                                                    .alpha(0.6f)
                                            )
                                        }
                                    }
                                }
                            }

                            ViewMode.HULU_STYLE -> {
                                HuluStyleView(
                                    items = items,
                                    onItemClick = { item ->
                                        if (item.isFolder) {
                                            onFolderSelected(item.id)
                                        } else {
                                            onMediaSelected(item)
                                        }
                                    },
                                    onScroll = { showTopBar = it }
                                )
                            }

                            ViewMode.HOME_VIDEO -> {
                                HomeVideoView(
                                    items = items,
                                    onItemClick = { item ->
                                        if (item.isFolder) {
                                            onFolderSelected(item.id)
                                        } else {
                                            onMediaSelected(item)
                                        }
                                    },
                                    viewModel = viewModel,
                                    onScroll = { showTopBar = it }
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
            Image(
                painter = painterResource(R.drawable.folder_icon),
                contentDescription = null,
                modifier = Modifier.size(102.dp)
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
                listOf(SortBy.SHOOT, SortBy.DATE).forEach { sortOption ->
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
                            text = if (sortOption == SortBy.SHOOT) "撮影日順" else "更新日時順"
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