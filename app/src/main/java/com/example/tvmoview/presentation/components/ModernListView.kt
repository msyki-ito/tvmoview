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
