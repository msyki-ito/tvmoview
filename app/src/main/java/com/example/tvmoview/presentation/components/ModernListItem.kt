package com.example.tvmoview.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import com.example.tvmoview.presentation.util.ImageLoaderProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.tvmoview.domain.model.MediaItem
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ModernListItem(
    item: MediaItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.thumbnailUrl != null) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = null,
                    imageLoader = ImageLoaderProvider.create(LocalContext.current),
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Icon(
                    imageVector = when {
                        item.isFolder -> Icons.Default.Folder
                        item.isVideo -> Icons.Default.PlayArrow
                        item.isImage -> Icons.Default.Image
                        else -> Icons.Default.Description
                    },
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (!item.isFolder) {
                    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
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
