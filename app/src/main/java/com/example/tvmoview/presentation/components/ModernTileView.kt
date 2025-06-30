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
    columnCount: Int,
    onItemClick: (MediaItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columnCount),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(items) { index, item ->
            // 表示優先度を設定
            val priority = when {
                index < 10 -> 1.0f  // 最初の10個は最優先
                index < 30 -> 0.5f  // 次の20個は中優先度
                else -> 0.1f        // それ以降は低優先度
            }

            ModernMediaCard(
                item = item,
                onClick = { onItemClick(item) },
                loadPriority = priority,
                showName = false
            )
        }
    }
}

//    // プリフェッチ設定
//    LaunchedEffect(gridState) {
//        snapshotFlow { gridState.firstVisibleItemIndex }
//            .collect { firstVisible ->
//                // 見える範囲の前後をプリフェッチ
//                val prefetchRange = 10
//                val start = (firstVisible - prefetchRange).coerceAtLeast(0)
//                val end = (firstVisible + prefetchRange).coerceAtMost(items.size - 1)
//
//                // ここでプリフェッチ処理を実行
//                Log.d("ModernTileView", "Prefetch range: $start to $end")
//            }
//    }
//}