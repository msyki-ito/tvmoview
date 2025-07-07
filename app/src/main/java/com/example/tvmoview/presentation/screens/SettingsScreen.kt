package com.example.tvmoview.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    var videoQuality by remember { mutableStateOf("高画質") }
    var autoPlay by remember { mutableStateOf(true) }
    var darkMode by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // ヘッダー
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "戻る",
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "⚙️ 設定",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 設定項目
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsSection(title = "再生設定") {
                    SettingsItem(
                        title = "自動再生",
                        description = "動画選択時に自動で再生を開始",
                        trailing = {
                            Switch(
                                checked = autoPlay,
                                onCheckedChange = { autoPlay = it }
                            )
                        }
                    )

                    SettingsItem(
                        title = "動画品質",
                        description = "再生時のデフォルト画質設定",
                        trailing = {
                            Text(
                                text = videoQuality,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        onClick = {
                            videoQuality = when (videoQuality) {
                                "低画質" -> "標準画質"
                                "標準画質" -> "高画質"
                                "高画質" -> "最高画質"
                                else -> "低画質"
                            }
                        }
                    )
                }
            }

            item {
                SettingsSection(title = "表示設定") {
                    SettingsItem(
                        title = "ダークモード",
                        description = "暗いテーマを使用",
                        trailing = {
                            Switch(
                                checked = darkMode,
                                onCheckedChange = { darkMode = it }
                            )
                        }
                    )
                }
            }

            item {
                SettingsSection(title = "アプリ情報") {
                    SettingsItem(
                        title = "バージョン",
                        description = "1.0.0",
                        trailing = null
                    )

                    SettingsItem(
                        title = "開発者",
                        description = "TV Movie Viewer Team",
                        trailing = null
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            content()
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    description: String,
    trailing: @Composable (() -> Unit)?,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick ?: {}
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (trailing != null) {
                trailing()
            }
        }
    }
}
