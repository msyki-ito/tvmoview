package com.example.tvmoview

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tvmoview.data.repository.MediaRepository
import androidx.room.Room
import com.example.tvmoview.data.local.AppDatabase
import com.example.tvmoview.data.repository.CacheAwareMediaRepository
import com.example.tvmoview.data.repository.StartupSyncUseCase
import com.example.tvmoview.presentation.screens.*
import com.example.tvmoview.presentation.theme.TVMovieTheme
import kotlinx.coroutines.launch

// OneDrive統合のための新しいimport
import com.example.tvmoview.data.auth.AuthenticationManager
import com.example.tvmoview.data.repository.OneDriveRepository

class MainActivity : ComponentActivity() {

    companion object {
        lateinit var mediaRepository: MediaRepository

        // OneDrive統合のための新しい変数
        lateinit var authManager: AuthenticationManager
        lateinit var oneDriveRepository: OneDriveRepository

        lateinit var database: AppDatabase
        lateinit var cacheRepository: CacheAwareMediaRepository
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 既存のテストデータ用（そのまま残す）
        mediaRepository = MediaRepository(this)

        // OneDrive統合の初期化（新規追加）
        authManager = AuthenticationManager(this)
        oneDriveRepository = OneDriveRepository(authManager)

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "cache.db"
        ).build()
        cacheRepository = CacheAwareMediaRepository(
            oneDriveRepository,
            database.cachedMediaItemDao(),
            database.folderSyncStatusDao()
        )

        Log.d("MainActivity", "🎉 TV Movie Viewer 完全版起動！")
        Log.d("MainActivity", "📁 OneDrive統合準備完了")

        setContent {
            TVMovieTheme {
                AuthenticationWrapper()
            }
        }

        // OAuth認証コールバック処理
        handleAuthRedirect(intent)

        val startupSync = StartupSyncUseCase(cacheRepository)
        lifecycleScope.launch { startupSync() }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleAuthRedirect(it) }
    }

    private fun handleAuthRedirect(intent: Intent) {
        val data = intent.data
        if (data?.scheme == "msauth" && data.host == "com.example.tvmoview") {
            val code = data.getQueryParameter("code")
            if (code != null) {
                Log.d("MainActivity", "🔐 認証コード受信: $code")
                // ここで認証コードをトークンに交換する処理が必要
                // 今回は簡略化してログのみ
            }
        }
    }
}

@Composable
fun AuthenticationWrapper() {
    var authState by remember { mutableStateOf<AuthState>(AuthState.Checking) }

    LaunchedEffect(Unit) {
        authState = if (MainActivity.authManager.isAuthenticated()) {
            AuthState.Authenticated
        } else {
            AuthState.NotAuthenticated
        }
    }

    when (authState) {
        AuthState.Checking -> {
            // 簡単なローディング表示
            Text("起動中...")
        }

        AuthState.Authenticated -> {
            AppNavigation()
        }

        AuthState.NotAuthenticated -> {
            LoginScreen(
                onLoginSuccess = {
                    authState = AuthState.Authenticated
                },
                onUseTestData = {
                    // テストデータ版で起動
                    authState = AuthState.Authenticated
                }
            )
        }
    }
}

sealed class AuthState {
    object Checking : AuthState()
    object Authenticated : AuthState()
    object NotAuthenticated : AuthState()
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        // ホーム画面（メディア一覧）
        composable("home") {
            ModernMediaBrowser(
                onMediaSelected = { mediaItem ->
                    if (mediaItem.isVideo) {
                        // OneDriveのdownloadUrlを含めて渡す（URL安全エンコード）
                        val encodedUrl = java.net.URLEncoder.encode(mediaItem.downloadUrl ?: "", "UTF-8")
                        Log.d("MainActivity", "🎬 動画選択: ${mediaItem.name}")
                        Log.d("MainActivity", "📊 downloadUrl: ${mediaItem.downloadUrl}")
                        navController.navigate("player/${mediaItem.id}/$encodedUrl")
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

        // フォルダ内表示
        composable(
            "folder/{folderId}",
            arguments = listOf(navArgument("folderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getString("folderId") ?: ""
            ModernMediaBrowser(
                folderId = folderId,
                onMediaSelected = { mediaItem ->
                    if (mediaItem.isVideo) {
                        // OneDriveのdownloadUrlを含めて渡す（URL安全エンコード）
                        val encodedUrl = java.net.URLEncoder.encode(mediaItem.downloadUrl ?: "", "UTF-8")
                        Log.d("MainActivity", "🎬 フォルダ内動画選択: ${mediaItem.name}")
                        Log.d("MainActivity", "📊 downloadUrl: ${mediaItem.downloadUrl}")
                        navController.navigate("player/${mediaItem.id}/$encodedUrl")
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

        // 動画プレイヤー（OneDrive downloadURL対応版）
        composable(
            "player/{itemId}/{downloadUrl}",
            arguments = listOf(
                navArgument("itemId") { type = NavType.StringType },
                navArgument("downloadUrl") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
            val encodedDownloadUrl = backStackEntry.arguments?.getString("downloadUrl") ?: ""

            // URLデコード（安全処理）
            val downloadUrl = try {
                java.net.URLDecoder.decode(encodedDownloadUrl, "UTF-8")
            } catch (e: Exception) {
                Log.w("MainActivity", "URL デコード失敗: $encodedDownloadUrl", e)
                ""
            }

            Log.d("MainActivity", "🎥 プレイヤー起動: itemId=$itemId")
            Log.d("MainActivity", "📺 downloadUrl=$downloadUrl")

            HighQualityPlayerScreen(
                itemId = itemId,
                downloadUrl = downloadUrl,
                onBack = { navController.popBackStack() }
            )
        }

        // 画像ビューア（追加）
        composable(
            "image/{itemId}",
            arguments = listOf(navArgument("itemId") { type = NavType.StringType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: ""

            // 画像ビューア（後で実装）
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("画像ビューア（未実装）\nitemId: $itemId")
            }
        }

        // 設定画面
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}