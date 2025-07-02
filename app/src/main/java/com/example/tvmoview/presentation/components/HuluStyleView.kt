package com.example.tvmoview.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.tvmoview.domain.model.MediaItem
import com.example.tvmoview.presentation.theme.HuluColors
import androidx.compose.runtime.*
import java.util.Calendar

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
        contentPadding = PaddingValues(vertical = 24.dp)
    ) {
        groupedItems.forEach { group ->
            item(key = group.date) {
                HuluDateHeader(
                    dateText = group.displayDate,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
            item(key = "${group.date}_content") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    items(items = group.items, key = { it.id }) { item ->
                        HuluMediaCard(
                            item = item,
                            onClick = { onItemClick(item) }
                        )
                    }
                }
            }
        }
    }
}

