package com.example.tvmoview.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.Icon
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
    val DateLabelBackground = Color.Black.copy(alpha = 0.6f)
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
        // メインプレビューエリア（45%に縮小）
        MainPreviewArea(
            selectedMedia = selectedMedia,
            onItemClick = onItemClick,
            modifier = Modifier.weight(0.45f)
        )

        // セクションリストエリア（55%に拡大）
        SectionListArea(
            sections = sections,
            selectedMedia = selectedMedia,
            onMediaSelected = { selectedMedia = it },
            onItemClick = onItemClick,
            modifier = Modifier.weight(0.55f)
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

            // オーバーレイ情報（選択後に表示）
            AnimatedVisibility(
                visible = showVideo || !media.isVideo,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                color = Color.Black.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = media.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = HomeVideoColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                                    .format(media.lastModified),
                                fontSize = 12.sp,
                                color = HomeVideoColors.TextSecondary
                            )
                            if (media.isVideo && media.duration > 0) {
                                Text(
                                    text = formatDuration(media.duration),
                                    fontSize = 12.sp,
                                    color = HomeVideoColors.TextSecondary
                                )
                            }
                            if (!media.isFolder) {
                                Text(
                                    text = media.formattedSize,
                                    fontSize = 12.sp,
                                    color = HomeVideoColors.TextSecondary
                                )
                            }
                        }
                        // 撮影場所情報の表示
                        // 注意: 現在のMediaItemモデルには撮影場所（GPS/EXIF）情報が含まれていません。
                        // 実装には以下が必要です：
                        // 1. MediaItemモデルに location: String? プロパティを追加
                        // 2. OneDriveRepository でメタデータ取得時にEXIF情報を解析
                        // 3. Android の ExifInterface または外部ライブラリで GPS 情報を取得
                        // 例: media.location?.let { location ->
                        //     Text(
                        //         text = "📍 $location",
                        //         fontSize = 12.sp,
                        //         color = HomeVideoColors.TextSecondary
                        //     )
                        // }
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
        verticalArrangement = Arrangement.spacedBy(4.dp), // 行間をさらに狭めて2行表示を確実に
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(
            items = sections,
            key = { section -> section.title } // 安定したキーを使用
        ) { section ->
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp), // 行の高さを140dpに縮小
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 日付ラベル（左側固定幅）
        DateLabel(
            date = section.items.firstOrNull()?.lastModified ?: Date(),
            modifier = Modifier
                .width(80.dp) // 72dp→80dpに拡張
                .height(120.dp) // 高さを明示的に指定
                .padding(start = 16.dp, end = 8.dp)
        )

        // 横スクロールカードリスト
        val listState = rememberLazyListState()
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(8.dp), // 間隔を8dpに縮小
            contentPadding = PaddingValues(start = 4.dp, end = 16.dp), // 左側の余白を追加
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 10.dp) // 上下に余白を追加してカードを中央寄せ
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
private fun DateLabel(
    date: Date,
    modifier: Modifier = Modifier
) {
    val calendar = Calendar.getInstance().apply { time = date }
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val month = SimpleDateFormat("MMM", Locale.getDefault()).format(date)
    val year = calendar.get(Calendar.YEAR)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp)
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp)
        ) {
            // 日（最も大きく）
            Text(
                text = String.format("%02d", day),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = HomeVideoColors.TextPrimary,
                lineHeight = 32.sp
            )
            // 月
            Text(
                text = month,
                fontSize = 16.sp,
                color = HomeVideoColors.TextPrimary,
                lineHeight = 16.sp
            )
            // 年
            Text(
                text = year.toString(),
                fontSize = 12.sp,
                color = HomeVideoColors.TextSecondary,
                lineHeight = 12.sp
            )
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
    val context = LocalContext.current

    // 初期選択時の自動フォーカス
    LaunchedEffect(isSelected) {
        if (isSelected && !isFocused) {
            focusRequester.requestFocus()
        }
    }

    // アニメーション値
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.1f else 1f,
        animationSpec = tween(200),
        label = "scale"
    )
    val elevation by animateDpAsState(
        targetValue = if (isFocused) 12.dp else 4.dp,
        animationSpec = tween(200),
        label = "elevation"
    )

    Card(
        modifier = Modifier
            .width((item.cardHeight.value * 0.9f * item.displayAspectRatio).dp) // サイズを90%に縮小
            .height((item.cardHeight.value * 0.9f).dp) // 高さも90%に
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
                        shape = RoundedCornerShape(6.dp)
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // サムネイル
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.thumbnailUrl ?: item.downloadUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // 動画の場合の再生時間表示
            if (item.isVideo && item.duration > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = formatDuration(item.duration),
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // フォルダの場合のアイコン表示
            if (item.isFolder) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center),
                    tint = Color.White.copy(alpha = 0.8f)
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