package com.example.tvmoview.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// SortBy import追加
import com.example.tvmoview.presentation.viewmodels.SortBy

@Composable
fun SortDialog(
    currentSort: SortBy,
    onSortSelected: (SortBy) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("並び順")
        },
        text = {
            Column {
                SortBy.values().forEach { sortOption ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = currentSort == sortOption,
                                onClick = { onSortSelected(sortOption) }
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSort == sortOption,
                            onClick = { onSortSelected(sortOption) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (sortOption) {
                                SortBy.NAME -> "名前順"
                                SortBy.DATE -> "更新日時順"
                                SortBy.SIZE -> "サイズ順"
                                SortBy.TYPE -> "種類順"
                            },
                            style = MaterialTheme.typography.bodyMedium
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