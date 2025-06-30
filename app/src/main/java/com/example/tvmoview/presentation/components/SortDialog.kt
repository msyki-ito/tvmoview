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
import com.example.tvmoview.presentation.viewmodels.SortOrder

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
                                SortBy.SHOOT -> "撮影日順"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = currentOrder == SortOrder.ASC,
                            onClick = { onOrderSelected(SortOrder.ASC) }
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentOrder == SortOrder.ASC,
                        onClick = { onOrderSelected(SortOrder.ASC) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("昇順")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = currentOrder == SortOrder.DESC,
                            onClick = { onOrderSelected(SortOrder.DESC) }
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentOrder == SortOrder.DESC,
                        onClick = { onOrderSelected(SortOrder.DESC) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("降順")
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