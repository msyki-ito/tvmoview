package com.example.tvmoview.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
// 必要に応じてインポートに追加
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tvmoview.R

// ViewMode import追加
import com.example.tvmoview.presentation.viewmodels.ViewMode
import com.example.tvmoview.presentation.viewmodels.SortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernTopBar(
    currentPath: String,
    viewMode: ViewMode,
    sortOrder: SortOrder,
    tileColumns: Int,
    onViewModeChange: () -> Unit,
    onTileColumnsChange: () -> Unit,
    onSortClick: () -> Unit,
    onOrderToggle: () -> Unit,
    onRefreshClick: () -> Unit,
    onSettingsClick: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    isLoading: Boolean = false
) {
    // 回転アニメーション
    val rotation by animateFloatAsState(
        targetValue = if (isLoading) 360f else 0f,
        animationSpec = if (isLoading) {
            infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        } else {
            tween(200)
        },
        label = "refresh_rotation"
    )

    TopAppBar(
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBackClick != null) {
                    IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                }

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "Logo",
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentPath,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isLoading) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "更新中...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        actions = {
            // 表示モード切り替えボタン
            IconButton(onClick = onViewModeChange) {
                Icon(
                    imageVector = when (viewMode) {
                        ViewMode.TILE -> Icons.Rounded.ViewList
                        ViewMode.HULU_STYLE -> Icons.Rounded.Tv  // TVアイコンに変更
                        ViewMode.HOME_VIDEO -> Icons.Rounded.ViewModule
                    },
                    contentDescription = when (viewMode) {
                        ViewMode.TILE -> "リスト表示に切り替え"
                        ViewMode.HULU_STYLE -> "ホームビデオ表示に切り替え"
                        ViewMode.HOME_VIDEO -> "タイル表示に切り替え"
                    }
                )
            }

            if (viewMode == ViewMode.TILE) {
                IconButton(onClick = onTileColumnsChange) {
                    Text(tileColumns.toString())
                }
            }

            IconButton(onClick = onOrderToggle) {
                Icon(
                    imageVector = if (sortOrder == SortOrder.ASC) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                    contentDescription = "並び順方向"
                )
            }

            // 更新ボタン（回転アニメーション付き）
            IconButton(
                onClick = { if (!isLoading) onRefreshClick() },
                enabled = !isLoading
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = if (isLoading) "更新中" else "更新",
                    modifier = Modifier.graphicsLayer {
                        rotationZ = rotation
                    }
                )
            }

            // ソートボタン
            IconButton(onClick = onSortClick) {
                Icon(
                    imageVector = Icons.Rounded.Sort,
                    contentDescription = "並び順"
                )
            }

            // 設定ボタン
            if (onSettingsClick != null) {
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "設定"
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}