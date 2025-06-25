package com.example.tvmoview.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.tvmoview.domain.model.MediaItem
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ModernMediaCard(
    item: MediaItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        item.isFolder -> Icons.Default.Folder
                        item.isVideo -> Icons.Default.PlayArrow
                        item.isImage -> Icons.Default.Image
                        else -> Icons.Default.Description
                    },
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (!item.isFolder) {
                    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                    Text(
                        text = "${formatFileSize(item.size)} • ${dateFormat.format(item.lastModified)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatFileSize(size: Long): String {
    val kb = 1024
    val mb = kb * 1024
    val gb = mb * 1024
    
    return when {
        size >= gb -> String.format("%.1f GB", size.toDouble() / gb)
        size >= mb -> String.format("%.1f MB", size.toDouble() / mb)
        size >= kb -> String.format("%.1f KB", size.toDouble() / kb)
        else -> "$size B"
    }
}
