package com.example.tvmoview.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import androidx.tv.foundation.lazy.list.focusRestorer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.tvmoview.domain.model.MediaItem
import com.example.tvmoview.presentation.theme.HuluColors
import com.example.tvmoview.presentation.viewmodels.DateGroup
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import java.util.Calendar

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HuluStyleView(
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val groupedItems by remember(items) {
        derivedStateOf {
            if (items.isEmpty()) {
                emptyList()
            } else {
                items.groupBy {
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
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(HuluColors.Background),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        groupedItems.forEach { group ->
            item(key = group.date) {
                HuluDateHeader(
                    dateText = group.displayDate,
                    itemCount = group.itemCount,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
            item(key = "${group.date}_content") {
                val listState = rememberTvLazyListState()
                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
                TvLazyRow(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .focusRestorer()
                        .focusRequester(focusRequester)
                        .padding(bottom = 16.dp)
                ) {
                    itemsIndexed(
                        items = group.items,
                        key = { _, item -> item.id },
                        contentType = { _, item -> if (item.isFolder) "folder" else "media" }
                    ) { index, item ->
                        HuluMediaCard(
                            item = item,
                            onClick = { onItemClick(item) },
                            modifier = Modifier
                                .then(if (index == 0) Modifier.focusRequester(focusRequester) else Modifier)
                                .animateItemPlacement(animationSpec = tween(durationMillis = 100))
                        )
                    }
                }
            }
        }
    }
}

