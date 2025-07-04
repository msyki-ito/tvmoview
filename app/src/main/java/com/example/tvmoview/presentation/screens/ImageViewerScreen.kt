package com.example.tvmoview.presentation.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import android.widget.Toast
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.imageLoader
import com.example.tvmoview.MainActivity
import com.example.tvmoview.domain.model.MediaItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ImageViewerScreen(
    currentImageId: String,
    folderId: String? = null,
    onBack: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var imageItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    val showInfo = remember { mutableStateOf(true) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Log.d("ImageViewer", "Screen started with imageId: $currentImageId, folderId: $folderId")

    LaunchedEffect(folderId) {
        isLoading = true
        try {
            val items = MainActivity.oneDriveRepository.getCachedItems(folderId)
                .filter { it.isImage }

            Log.d("ImageViewer", "Found ${items.size} images")
            imageItems = items

            val foundIndex = items.indexOfFirst { it.id == currentImageId }
            currentIndex = if (foundIndex >= 0) foundIndex else 0

            Log.d("ImageViewer", "Current index: $currentIndex")

            if (items.isNotEmpty() && currentIndex >= 0 && currentIndex < items.size) {
                val currentItem = items[currentIndex]
                Log.d("ImageViewer", "Loading image: ${currentItem.name}")

                val url = MainActivity.oneDriveRepository
                    .getDownloadUrl(currentItem.id)
                    ?: currentItem.downloadUrl
                imageUrl = url
                Log.d("ImageViewer", "Image URL: $url")
            }
        } catch (e: Exception) {
            Log.e("ImageViewer", "Error loading images", e)
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(currentIndex) {
        if (imageItems.isNotEmpty() && currentIndex >= 0 && currentIndex < imageItems.size) {
            scope.launch {
                val currentItem = imageItems[currentIndex]
                Log.d("ImageViewer", "Switching to image: ${currentItem.name}")

                val url = MainActivity.oneDriveRepository
                    .getDownloadUrl(currentItem.id)
                    ?: currentItem.downloadUrl
                imageUrl = url
                prefetchImage(currentIndex + 1)
                prefetchImage(currentIndex - 1)
            }
        }
    }

    LaunchedEffect(imageUrl) {
        showInfo.value = true
        delay(3000)
        showInfo.value = false
    }

    suspend fun prefetchImage(index: Int) {
        if (index < 0 || index >= imageItems.size) return
        val item = imageItems[index]
        val url = MainActivity.oneDriveRepository
            .getDownloadUrl(item.id)
            ?: item.downloadUrl
        context.imageLoader.enqueue(
            ImageRequest.Builder(context)
                .data(url)
                .build()
        )
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    BackHandler { onBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    when (keyEvent.key) {
                        Key.Enter -> {
                            scale = if (scale < 5f) scale + 1f else 1f
                            true
                        }
                        Key.DirectionLeft -> {
                            if (scale > 1f) {
                                offsetX = (offsetX + 40f).coerceIn(-1000f, 1000f)
                            } else if (currentIndex > 0) {
                                currentIndex--
                                Log.d("ImageViewer", "Navigate left to index: $currentIndex")
                            }
                            true
                        }
                        Key.DirectionRight -> {
                            if (scale > 1f) {
                                offsetX = (offsetX - 40f).coerceIn(-1000f, 1000f)
                            } else if (currentIndex < imageItems.size - 1) {
                                currentIndex++
                                Log.d("ImageViewer", "Navigate right to index: $currentIndex")
                            }
                            true
                        }
                        Key.DirectionUp -> {
                            if (scale > 1f) {
                                offsetY = (offsetY + 40f).coerceIn(-1000f, 1000f)
                                true
                            } else false
                        }
                        Key.DirectionDown -> {
                            if (scale > 1f) {
                                offsetY = (offsetY - 40f).coerceIn(-1000f, 1000f)
                                true
                            } else false
                        }
                        Key.M -> {
                            folderId?.let {
                                scope.launch {
                                    MainActivity.oneDriveRepository.setFolderCover(it, imageItems[currentIndex].id)
                                    Toast.makeText(context, "カバー画像を設定しました", Toast.LENGTH_SHORT).show()
                                }
                            }
                            true
                        }
                        Key.Back, Key.Escape -> {
                            Log.d("ImageViewer", "Back pressed")
                            onBack()
                            true
                        }
                        else -> false
                    }
                } else false
            }
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            imageUrl != null -> {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = if (imageItems.isNotEmpty() && currentIndex < imageItems.size) {
                        imageItems[currentIndex].name
                    } else {
                        null
                    },
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        },
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color.White)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("画像を読み込み中...", color = Color.White)
                            }
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.BrokenImage,
                                    contentDescription = "エラー",
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("画像を読み込めませんでした", color = Color.White)
                            }
                        }
                    }
                )

                if (showInfo.value && imageItems.isNotEmpty() && currentIndex < imageItems.size) {
                    val item = imageItems[currentIndex]
                    val dateFormat = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault())
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(8.dp)
                    ) {
                        Text(item.name, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        Text(dateFormat.format(item.lastModified), color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }
                }

            }
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("画像が見つかりません", color = Color.White)
                }
            }
        }
    }
}
