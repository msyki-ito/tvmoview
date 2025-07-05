package com.example.tvmoview.presentation.components

import androidx.compose.foundation.layout.*
import androidx.tv.foundation.lazy.grid.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.tv.foundation.ExperimentalTvFoundationApi
import androidx.tv.foundation.lazy.grid.focusRestorer
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.tvmoview.domain.model.MediaItem

@OptIn(ExperimentalTvFoundationApi::class)
@Composable
fun ModernTileView(
    items: List<MediaItem>,
    columnCount: Int,
    state: TvLazyGridState,
    onItemClick: (MediaItem) -> Unit,
    focusedId: String?,
    onItemFocused: (String) -> Unit,
    focusRequester: FocusRequester
) {
    TvLazyVerticalGrid(
        columns = GridCells.Fixed(columnCount),
        state = state,
        modifier = Modifier.focusRestorer { focusRequester },
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(items) { index, item ->
            // 表示優先度を設定
            val priority = when {
                index < 10 -> 1.0f  // 最初の10個は最優先
                index < 30 -> 0.5f  // 次の20個は中優先度
                else -> 0.1f        // それ以降は低優先度
            }

            val mod = if (item.id == focusedId) Modifier.focusRequester(focusRequester) else Modifier
            ModernMediaCard(
                item = item,
                onClick = { onItemClick(item) },
                loadPriority = priority,
                showName = item.isFolder || (!item.isVideo && !item.isImage),
                modifier = mod.onFocusChanged { if (it.isFocused) onItemFocused(item.id) }
            )
        }
    }
}