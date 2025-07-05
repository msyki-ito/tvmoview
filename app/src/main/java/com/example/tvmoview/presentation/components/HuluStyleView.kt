package com.example.tvmoview.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.tv.foundation.ExperimentalTvFoundationApi
import androidx.tv.foundation.lazy.list.focusRestorer
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.tvmoview.domain.model.MediaItem
import com.example.tvmoview.presentation.theme.HuluColors
import com.example.tvmoview.presentation.viewmodels.DateGroup
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import java.util.Calendar

@OptIn(ExperimentalComposeUiApi::class, ExperimentalTvFoundationApi::class)
@Composable
fun HuluStyleView(
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    focusedId: String?,
    onItemFocused: (String) -> Unit,
    focusRequester: FocusRequester
) {
    val (folders, media) = remember(items) {
        items.partition { it.isFolder }
    }
    val sortedFolders = remember(folders) { folders.sortedBy { it.name.lowercase() } }
    val groupedItems = remember(media) {
        media.groupBy {
            Calendar.getInstance().apply {
                time = it.lastModified
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
        }.map { DateGroup(it.key, it.value) }
            .sortedByDescending { it.date }
    }

    TvLazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(HuluColors.Background)
            .focusRestorer { focusRequester },
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        if (sortedFolders.isNotEmpty()) {
            item(key = "folders_header") {
                HuluDateHeader(
                    dateText = "フォルダ一覧",
                    itemCount = sortedFolders.size,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
            item(key = "folders_row") {
                val rowState = rememberTvLazyListState()
                val rowFocusRequester = remember { FocusRequester() }
                TvLazyRow(
                    state = rowState,
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .focusRestorer { rowFocusRequester }
                ) {
                    itemsIndexed(sortedFolders, key = { _, it -> it.id }) { index, item ->
                        val mod = when {
                            item.id == focusedId -> Modifier.focusRequester(focusRequester)
                            index == 0 -> Modifier.focusRequester(rowFocusRequester)
                            else -> Modifier
                        }
                        HuluMediaCard(
                            item = item,
                            onClick = { onItemClick(item) },
                            modifier = mod,
                            onFocused = { onItemFocused(item.id) }
                        )
                    }
                }
            }
        }
        groupedItems.forEach { group ->
            item(key = group.date) {
                HuluDateHeader(
                    dateText = group.displayDate,
                    itemCount = group.itemCount,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
            item(key = "${group.date}_content") {
                val rowState = rememberTvLazyListState()
                val rowFocusRequester = remember { FocusRequester() }
                TvLazyRow(
                    state = rowState,
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .focusRestorer { rowFocusRequester }
                ) {
                    itemsIndexed(items = group.items, key = { _, it -> it.id }) { index, item ->
                        val mod = when {
                            item.id == focusedId -> Modifier.focusRequester(focusRequester)
                            index == 0 -> Modifier.focusRequester(rowFocusRequester)
                            else -> Modifier
                        }
                        HuluMediaCard(
                            item = item,
                            onClick = { onItemClick(item) },
                            modifier = mod,
                            onFocused = { onItemFocused(item.id) }
                        )
                    }
                }
            }
        }
    }
}

