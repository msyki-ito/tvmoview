# PowerShell用スクリプト (create_files.ps1)
# 使用方法: プロジェクトのルートディレクトリで実行
# .\create_files.ps1

$basePath = "app\src\main\java\com\example\tvmoview"

# ディレクトリ構造を作成
$directories = @(
    "$basePath\data\repository",
    "$basePath\domain\model",
    "$basePath\presentation\screens",
    "$basePath\presentation\components",
    "$basePath\presentation\viewmodels",
    "$basePath\presentation\theme",
    "$basePath\tv"
)

foreach ($dir in $directories) {
    New-Item -ItemType Directory -Path $dir -Force
    Write-Host "ディレクトリ作成: $dir"
}

# MainActivity.kt
$mainActivity = @'
package com.example.tvmoview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tvmoview.data.repository.MediaRepository
import com.example.tvmoview.presentation.screens.*
import com.example.tvmoview.presentation.theme.OneDriveTVViewerTheme
import com.example.tvmoview.presentation.viewmodels.MediaBrowserViewModel
import com.example.tvmoview.presentation.viewmodels.MediaBrowserViewModelFactory
import com.example.tvmoview.tv.FireTVOptimizations

class MainActivity : ComponentActivity() {

    companion object {
        lateinit var mediaRepository: MediaRepository
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mediaRepository = MediaRepository(this)
        FireTVOptimizations.initializeForFireTV(this)

        setContent {
            OneDriveTVViewerTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            ModernMediaBrowser(
                onMediaSelected = { mediaItem ->
                    if (mediaItem.isVideo) {
                        navController.navigate("player/${mediaItem.id}/${mediaItem.name}")
                    } else if (mediaItem.isImage) {
                        navController.navigate("image/${mediaItem.id}")
                    }
                },
                onFolderSelected = { folderId ->
                    navController.navigate("folder/$folderId")
                },
                onSettingsClick = {
                    navController.navigate("settings")
                }
            )
        }

        composable(
            "folder/{folderId}",
            arguments = listOf(navArgument("folderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getString("folderId") ?: ""
            ModernMediaBrowser(
                folderId = folderId,
                onMediaSelected = { mediaItem ->
                    if (mediaItem.isVideo) {
                        navController.navigate("player/${mediaItem.id}/${mediaItem.name}")
                    } else if (mediaItem.isImage) {
                        navController.navigate("image/${mediaItem.id}")
                    }
                },
                onFolderSelected = { childFolderId ->
                    navController.navigate("folder/$childFolderId")
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            "player/{itemId}/{itemName}",
            arguments = listOf(
                navArgument("itemId") { type = NavType.StringType },
                navArgument("itemName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: ""

            HighQualityPlayerScreen(
                itemId = itemId,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
'@

# MediaItem.kt
$mediaItem = @'
package com.example.tvmoview.domain.model

import java.util.*

data class MediaItem(
    val id: String,
    val name: String,
    val size: Long,
    val lastModified: Date,
    val dateTaken: Date? = null,
    val mimeType: String? = null,
    val isFolder: Boolean = false,
    val thumbnailUrl: String? = null,
    val downloadUrl: String? = null
) {
    val isVideo: Boolean
        get() = mimeType?.startsWith("video/") == true
    
    val isImage: Boolean
        get() = mimeType?.startsWith("image/") == true
}

enum class ViewMode {
    TILE, LIST
}

enum class SortBy {
    NAME, DATE_MODIFIED, DATE_TAKEN, SIZE
}
'@

# MediaRepository.kt
$mediaRepository = @'
package com.example.tvmoview.data.repository

import android.content.Context
import com.example.tvmoview.domain.model.MediaItem
import kotlinx.coroutines.delay
import java.util.*

class MediaRepository(private val context: Context) {

    suspend fun getRootItems(): List<MediaItem> {
        delay(1000)

        return listOf(
            MediaItem(
                id = "folder1",
                name = "写真",
                size = 0,
                lastModified = Date(),
                isFolder = true
            ),
            MediaItem(
                id = "folder2",
                name = "動画",
                size = 0,
                lastModified = Date(),
                isFolder = true
            ),
            MediaItem(
                id = "video1",
                name = "サンプル動画.mp4",
                size = 52428800,
                lastModified = Date(),
                mimeType = "video/mp4"
            ),
            MediaItem(
                id = "image1",
                name = "風景写真.jpg",
                size = 3145728,
                lastModified = Date(),
                mimeType = "image/jpeg"
            )
        )
    }

    suspend fun getFolderItems(folderId: String): List<MediaItem> {
        delay(800)

        return when (folderId) {
            "folder1" -> {
                (1..10).map { i ->
                    MediaItem(
                        id = "image_$i",
                        name = "写真$i.jpg",
                        size = (1000000..5000000).random().toLong(),
                        lastModified = Date(System.currentTimeMillis() - i * 86400000),
                        mimeType = "image/jpeg"
                    )
                }
            }
            "folder2" -> {
                (1..5).map { i ->
                    MediaItem(
                        id = "video_$i",
                        name = "動画$i.mp4",
                        size = (50000000..200000000).random().toLong(),
                        lastModified = Date(System.currentTimeMillis() - i * 86400000),
                        mimeType = "video/mp4"
                    )
                }
            }
            else -> emptyList()
        }
    }

    fun getCurrentPath(folderId: String?): String {
        return when (folderId) {
            "folder1" -> "写真"
            "folder2" -> "動画"
            else -> ""
        }
    }
}
'@

# MediaBrowserViewModelFactory.kt
$viewModelFactory = @'
package com.example.tvmoview.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.tvmoview.data.repository.MediaRepository

class MediaBrowserViewModelFactory(
    private val repository: MediaRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MediaBrowserViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MediaBrowserViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
'@

# MediaBrowserViewModel.kt
$viewModel = @'
package com.example.tvmoview.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tvmoview.data.repository.MediaRepository
import com.example.tvmoview.domain.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MediaBrowserViewModel(
    private val repository: MediaRepository
) : ViewModel() {

    private val _items = MutableStateFlow<List<MediaItem>>(emptyList())
    val items: StateFlow<List<MediaItem>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.TILE)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    private val _sortBy = MutableStateFlow(SortBy.NAME)
    val sortBy: StateFlow<SortBy> = _sortBy.asStateFlow()

    private val _currentPath = MutableStateFlow("")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    fun loadItems(folderId: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val fetchedItems = if (folderId == null) {
                    repository.getRootItems()
                } else {
                    repository.getFolderItems(folderId)
                }

                _items.value = sortItems(fetchedItems, _sortBy.value)
                _currentPath.value = repository.getCurrentPath(folderId)
            } catch (e: Exception) {
                // エラー処理
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleViewMode() {
        _viewMode.value = when (_viewMode.value) {
            ViewMode.TILE -> ViewMode.LIST
            ViewMode.LIST -> ViewMode.TILE
        }
    }

    fun setSortBy(sortBy: SortBy) {
        _sortBy.value = sortBy
        _items.value = sortItems(_items.value, sortBy)
    }

    private fun sortItems(items: List<MediaItem>, sortBy: SortBy): List<MediaItem> {
        return when (sortBy) {
            SortBy.NAME -> items.sortedBy { it.name.lowercase() }
            SortBy.DATE_MODIFIED -> items.sortedByDescending { it.lastModified }
            SortBy.DATE_TAKEN -> items.sortedByDescending { it.dateTaken ?: it.lastModified }
            SortBy.SIZE -> items.sortedByDescending { it.size }
        }
    }
}
'@

# ファイル作成のハッシュテーブル
$files = @{
    "$basePath\MainActivity.kt" = $mainActivity
    "$basePath\domain\model\MediaItem.kt" = $mediaItem
    "$basePath\data\repository\MediaRepository.kt" = $mediaRepository
    "$basePath\presentation\viewmodels\MediaBrowserViewModelFactory.kt" = $viewModelFactory
    "$basePath\presentation\viewmodels\MediaBrowserViewModel.kt" = $viewModel
}

# 基本ファイルを作成
foreach ($file in $files.GetEnumerator()) {
    $file.Value | Out-File -FilePath $file.Key -Encoding UTF8
    Write-Host "ファイル作成: $($file.Key)"
}

# 残りのファイル（短いもの）を作成
$shortFiles = @{
    "$basePath\tv\FireTVOptimizations.kt" = @'
package com.example.tvmoview.tv

import android.app.Activity

object FireTVOptimizations {
    fun initializeForFireTV(activity: Activity) {
        // Fire TV固有の最適化をここに実装
    }
}
'@

    "$basePath\presentation\theme\OneDriveTVViewerTheme.kt" = @'
package com.example.tvmoview.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

@Composable
fun OneDriveTVViewerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = darkColorScheme(),
        content = content
    )
}
'@

    "$basePath\presentation\screens\HighQualityPlayerScreen.kt" = @'
package com.example.tvmoview.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HighQualityPlayerScreen(
    itemId: String,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "動画プレイヤー",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Item ID: $itemId",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onBack) {
                Text("戻る")
            }
        }
    }
}
'@

    "$basePath\presentation\screens\SettingsScreen.kt" = @'
package com.example.tvmoview.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "設定",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onBack) {
                Text("戻る")
            }
        }
    }
}
'@
}

foreach ($file in $shortFiles.GetEnumerator()) {
    $file.Value | Out-File -FilePath $file.Key -Encoding UTF8
    Write-Host "ファイル作成: $($file.Key)"
}

Write-Host "`n✅ 基本ファイルの作成が完了しました！"
Write-Host "次に components フォルダのファイルを作成します..."

# Componentsファイル作成用の第2スクリプトも提示
Write-Host "`n🔄 残りのコンポーネントファイルを作成するには:"
Write-Host ".\create_components.ps1 を実行してください"
