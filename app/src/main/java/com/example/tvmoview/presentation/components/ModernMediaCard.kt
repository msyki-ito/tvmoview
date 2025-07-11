﻿package com.example.tvmoview.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.example.tvmoview.presentation.theme.HuluColors
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import com.example.tvmoview.domain.model.MediaItem
import com.example.tvmoview.R
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ModernMediaCard(
    item: MediaItem,
    onClick: () -> Unit,
    loadPriority: Float = 0.5f,
    showName: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = HuluColors.CardBackground)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {  // ColumnからBoxに変更
            // サムネイル/アイコン表示部分
            val context = LocalContext.current

            when {
                item.thumbnailUrl != null || (item.isVideo || item.isImage) -> {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(item.thumbnailUrl ?: generateVideoThumbnail(item))
                            .diskCacheKey("thumb_${item.id}")
                            .crossfade(300)
                            .size(800, 600)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        shimmerBrush(targetValue = 1300f)
                                    )
                            )
                        },
                        error = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when {
                                        item.isVideo -> Icons.Default.PlayArrow
                                        item.isImage -> Icons.Default.Image
                                        else -> Icons.Default.Description
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                    if (item.isVideo) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(48.dp)
                        )
                        Text(
                            text = formatTime(item.duration),
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(4.dp)
                        )
                    }
                }
                item.isFolder -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.folder_icon),
                            contentDescription = null,
                            modifier = Modifier.size(76.dp)
                        )
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // ファイル名表示（フォルダとドキュメントのみ）
            if (showName) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(
                            Color.Black.copy(alpha = 0.7f)  // 半透明の黒背景
                        )
                        .padding(8.dp)
                ) {
                    Column {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!item.isFolder && !item.isVideo && !item.isImage) {
                            Text(
                                text = formatFileSize(item.size),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// 動画サムネイルURL生成
private fun generateVideoThumbnail(item: MediaItem): String? {
    return if (item.isVideo && item.id.isNotEmpty() && !item.id.startsWith("test")) {
        "https://graph.microsoft.com/v1.0/me/drive/items/${item.id}/thumbnails/0/large/content"
    } else {
        item.thumbnailUrl
    }
}

// シマーエフェクト用ブラシ
@Composable
fun shimmerBrush(targetValue: Float = 1000f, showShimmer: Boolean = false): Brush {
    return if (showShimmer) {
        val shimmerColors = listOf(
            Color.LightGray.copy(alpha = 0.6f),
            Color.LightGray.copy(alpha = 0.2f),
            Color.LightGray.copy(alpha = 0.6f),
        )

        val transition = rememberInfiniteTransition(label = "shimmer")
        val translateAnimation = transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "shimmer"
        )
        Brush.linearGradient(
            colors = shimmerColors,
            start = Offset.Zero,
            end = Offset(x = translateAnimation.value, y = translateAnimation.value)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.LightGray.copy(alpha = 0.3f),
                Color.LightGray.copy(alpha = 0.3f)
            )
        )
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

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}