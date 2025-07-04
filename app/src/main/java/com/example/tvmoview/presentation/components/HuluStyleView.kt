package com.example.tvmoview.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.focus.onFocusEvent
import kotlinx.coroutines.launch
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HuluStyleView(
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val groupedItems = remember(items) {
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
                val rowState = rememberLazyListState()
                val rowFocusRequester = remember { FocusRequester() }
                val scope = rememberCoroutineScope()
                LazyRow(
                    state = rowState,
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .focusGroup()
                        .focusRestorer { rowFocusRequester }
                ) {
                    itemsIndexed(items = group.items, key = { _, it -> it.id }) { index, item ->
                        HuluMediaCard(
                            item = item,
                            onClick = { onItemClick(item) },
                            modifier = (if (index == 0) Modifier.focusRequester(rowFocusRequester) else Modifier)
                                .onFocusEvent { if (it.isFocused) scope.launch { rowState.animateScrollToItem(index) } }
                        )
                    }
                }
                LaunchedEffect(Unit) {
                    rowFocusRequester.requestFocus()
                }
            }
        }
    }
}

