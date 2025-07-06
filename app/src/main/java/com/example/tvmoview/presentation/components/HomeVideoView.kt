package com.example.tvmoview.presentation.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.tvmoview.domain.model.MediaItem
import com.example.tvmoview.presentation.viewmodels.DateGroup
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.text.SimpleDateFormat
import java.util.*

// カラー定義
object HomeVideoColors {
    val BackgroundPrimary = Color(0xFF0B0C0F)
    val CardBorderFocus = Color.White
    val TextPrimary = Color.White.copy(alpha = 0.9f)
    val TextSecondary = Color.White.copy(alpha = 0.7f)
    val ShadowColor = Color.Black.copy(alpha = 0.6f)
}

@Composable
fun HomeVideoView(
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    // 選択中のメディア状態
    var selectedMedia by remember { mutableStateOf<MediaItem?>(null) }

    // セクション分け（日付でグループ化）
    val sections = remember(items) {
        val (folders, media) = items.partition { it.isFolder }
        val folderSection = if (folders.isNotEmpty()) {
            DateGroup(Date(), folders).let {
                MediaSection("フォルダ", folders)
            }
        } else null

        val mediaSections = media.groupBy { item ->
            Calendar.getInstance().apply {
                time = item.lastModified
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
        }.map { (date, items) ->
            val dateFormat = SimpleDateFormat("yyyy年M月d日", Locale.JAPAN)
            MediaSection(dateFormat.format(date), items.sortedByDescending { it.lastModified })
        }.sortedByDescending { it.items.firstOrNull()?.lastModified }

        listOfNotNull(folderSection) + mediaSections
    }

    // 初期選択
    LaunchedEffect(sections) {
        if (selectedMedia == null && sections.isNotEmpty()) {
            selectedMedia = sections.firstOrNull()?.items?.firstOrNull()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HomeVideoColors.BackgroundPrimary)
    ) {
        // メインプレビューエリア（55%）
        MainPreviewArea(
            selectedMedia = selectedMedia,
            onItemClick = onItemClick,
            modifier = Modifier.weight(0.55f)
        )

        // セクションリストエリア（45%）
        SectionListArea(
            sections = sections,
            selectedMedia = selectedMedia,
            onMediaSelected = { selectedMedia = it },
            onItemClick = onItemClick,
            modifier = Modifier.weight(0.45f)
        )
    }
}

@Composable
private fun MainPreviewArea(
    selectedMedia: MediaItem?,
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showVideo by remember { mutableStateOf(false) }
    var videoUrl by remember { mutableStateOf<String?>(null) }

    // 自動プレビュー開始（500msディレイ）とURL取得
    LaunchedEffect(selectedMedia) {
        showVideo = false
        videoUrl = null
        if (selectedMedia?.isVideo == true) {
            // OneDriveからダウンロードURL取得
            val url = com.example.tvmoview.MainActivity.oneDriveRepository
                .getDownloadUrl(selectedMedia.id)
            if (url != null) {
                videoUrl = url
                delay(500)
                showVideo = true
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
    ) {
        selectedMedia?.let { media ->
            Crossfade(
                targetState = media to showVideo,
                animationSpec = tween(200)
            ) { (currentMedia, shouldShowVideo) ->
                when {
                    shouldShowVideo && currentMedia.isVideo -> {
                        // 動画プレビュー
                        videoUrl?.let { url ->
                            VideoPreview(
                                videoUrl = url,
                                modifier = Modifier.fillMaxSize()
                            )
                        } ?: Box(Modifier.fillMaxSize()) // URLがまだない場合は空Box
                    }
                    else -> {
                        // 静止画表示
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(currentMedia.thumbnailUrl ?: currentMedia.downloadUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = currentMedia.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // オーバーレイ情報
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    Text(
                        text = media.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = HomeVideoColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                                .format(media.lastModified),
                            fontSize = 14.sp,
                            color = HomeVideoColors.TextSecondary
                        )
                        if (media.isVideo && media.duration > 0) {
                            Text(
                                text = formatDuration(media.duration),
                                fontSize = 12.sp,
                                color = HomeVideoColors.TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoPreview(
    videoUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(ExoMediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = true
            volume = 0f // ミュート
            repeatMode = ExoPlayer.REPEAT_MODE_ONE // ループ再生
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
            }
        },
        modifier = modifier
    )
}

@Composable
private fun SectionListArea(
    sections: List<MediaSection>,
    selectedMedia: MediaItem?,
    onMediaSelected: (MediaItem) -> Unit,
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        items(sections) { section ->
            SectionRow(
                section = section,
                selectedMedia = selectedMedia,
                onMediaSelected = onMediaSelected,
                onItemClick = onItemClick
            )
        }
    }
}

@Composable
private fun SectionRow(
    section: MediaSection,
    selectedMedia: MediaItem?,
    onMediaSelected: (MediaItem) -> Unit,
    onItemClick: (MediaItem) -> Unit
) {
    Column {
        // セクションヘッダー
        Text(
            text = section.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = HomeVideoColors.TextPrimary,
            modifier = Modifier.padding(start = 48.dp, end = 48.dp, bottom = 8.dp)
        )

        // 横スクロールカードリスト
        val listState = rememberLazyListState()
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 48.dp)
        ) {
            items(section.items, key = { it.id }) { item ->
                MediaCard(
                    item = item,
                    isSelected = item.id == selectedMedia?.id,
                    onFocus = { onMediaSelected(item) },
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

@Composable
private fun MediaCard(
    item: MediaItem,
    isSelected: Boolean,
    onFocus: () -> Unit,
    onClick: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    // アニメーション値
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.25f else 1f,
        animationSpec = tween(200),
        label = "scale"
    )
    val elevation by animateDpAsState(
        targetValue = if (isFocused) 12.dp else 0.dp,
        animationSpec = tween(200),
        label = "elevation"
    )

    Card(
        modifier = Modifier
            .width(160.dp)
            .height(90.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused) {
                    onFocus()
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (isFocused) {
                    Modifier.border(
                        width = 2.dp,
                        color = HomeVideoColors.CardBorderFocus,
                        shape = RoundedCornerShape(8.dp)
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        onClick = onClick
    ) {
        Box {
            // サムネイル
            AsyncImage(
                model = item.thumbnailUrl ?: item.downloadUrl,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // タイトル表示
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .background(HomeVideoColors.ShadowColor)
                    .padding(4.dp)
            ) {
                Text(
                    text = item.name,
                    fontSize = 12.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// データクラス
data class MediaSection(
    val title: String,
    val items: List<MediaItem>
)

// ユーティリティ関数
private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60

    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
        else -> String.format("%d:%02d", minutes, seconds)
    }
}