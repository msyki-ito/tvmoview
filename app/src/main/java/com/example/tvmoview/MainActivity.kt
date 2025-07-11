﻿package com.example.tvmoview

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import com.example.tvmoview.presentation.screens.SplashScreen
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tvmoview.data.repository.MediaRepository
import com.example.tvmoview.presentation.screens.*
import com.example.tvmoview.presentation.theme.TVMovieTheme
import com.example.tvmoview.presentation.viewmodels.MediaBrowserViewModel

// OneDrive統合のための新しいimport
import com.example.tvmoview.data.auth.AuthenticationManager
import com.example.tvmoview.data.repository.OneDriveRepository
import com.example.tvmoview.data.db.MediaDatabaseProvider
import com.example.tvmoview.data.prefs.UserPreferences
import coil.ImageLoader
import coil.Coil
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.OkHttpClient

class MainActivity : ComponentActivity() {

    companion object {
        lateinit var mediaRepository: MediaRepository

        // OneDrive統合のための新しい変数
        lateinit var authManager: AuthenticationManager
        lateinit var oneDriveRepository: OneDriveRepository
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 既存のテストデータ用（そのまま残す）
        mediaRepository = MediaRepository(this)

        UserPreferences.init(this)

        // OneDrive統合の初期化（新規追加）
        authManager = AuthenticationManager(this)
        MediaDatabaseProvider.init(this)
        oneDriveRepository = OneDriveRepository(
            authManager,
            MediaDatabaseProvider.database.mediaDao(),
            MediaDatabaseProvider.database.folderSyncDao(),
            MediaDatabaseProvider.database.folderCoverDao()
        )

        val imageLoader = ImageLoader.Builder(this)
            .diskCache(
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(30L * 1024 * 1024) // 30MB
                    .build()
            )
            .memoryCache(
                MemoryCache.Builder(this)
                    .maxSizePercent(0.08)
                    .weakReferencesEnabled(true)
                    .build()
            )
            .okHttpClient {
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val request = chain.request()
                        val url = request.url.toString()
                        val newRequest = if (url.contains("graph.microsoft.com") ||
                            url.contains("onedrive")) {
                            val token = kotlinx.coroutines.runBlocking { authManager.getValidToken() }
                            if (token != null) {
                                Log.d("Coil", "Adding auth header for: ${url.take(50)}...")
                                request.newBuilder()
                                    .addHeader("Authorization", "Bearer ${token.accessToken}")
                                    .build()
                            } else {
                                Log.w("Coil", "No valid token for: ${url.take(50)}...")
                                request
                            }
                        } else {
                            request
                        }
                        chain.proceed(newRequest)
                    }
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()
        Coil.setImageLoader(imageLoader)

        Log.d("MainActivity", "🎉 TV Movie Viewer 完全版起動！")
        Log.d("MainActivity", "📁 OneDrive統合準備完了")

        setContent {
            var showSplash by remember { mutableStateOf(true) }

            TVMovieTheme {
                if (showSplash) {
                    SplashScreen(onFinished = { showSplash = false })
                } else {
                    AuthenticationWrapper()
                }
            }
        }

        // OAuth認証コールバック処理
        handleAuthRedirect(intent)
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
        AuthState.Checking -> SplashScreen(onFinished = {})

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
    val owner = LocalContext.current as ViewModelStoreOwner
    val sharedViewModel: MediaBrowserViewModel = viewModel(viewModelStoreOwner = owner)

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        // ホーム画面（メディア一覧）
        composable("home") {
            ModernMediaBrowser(
                viewModel = sharedViewModel,
                onMediaSelected = { mediaItem ->
                    if (mediaItem.isVideo) {
                        Log.d("MainActivity", "🎬 動画選択: ${mediaItem.name}")
                        navController.navigate("player/${mediaItem.id}")
                    } else if (mediaItem.isImage) {
                        Log.d("MainActivity", "🖼️ 画像選択: ${mediaItem.name}")
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
                viewModel = sharedViewModel,
                folderId = folderId,
                onMediaSelected = { mediaItem ->
                    if (mediaItem.isVideo) {
                        Log.d("MainActivity", "🎬 フォルダ内動画選択: ${mediaItem.name}")
                        navController.navigate("player/${mediaItem.id}")
                    } else if (mediaItem.isImage) {
                        Log.d("MainActivity", "🖼️ フォルダ内画像選択: ${mediaItem.name}")
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

        // 動画プレイヤー（downloadURLは再生時に取得）
        composable(
            "player/{itemId}",
            arguments = listOf(
                navArgument("itemId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
            Log.d("MainActivity", "🎥 プレイヤー起動: itemId=$itemId")

            HighQualityPlayerScreen(
                itemId = itemId,
                onBack = { navController.popBackStack() },
                viewModel = sharedViewModel
            )
        }

        // 画像ビューア
        composable(
            "image/{itemId}",
            arguments = listOf(navArgument("itemId") { type = NavType.StringType })
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
            val parentFolderId = navController.previousBackStackEntry?.arguments?.getString("folderId")

            Log.d("MainActivity", "🖼️ 画像ビューワー起動: itemId=$itemId, parentFolder=$parentFolderId")

            ImageViewerScreen(
                currentImageId = itemId,
                folderId = parentFolderId,
                onBack = { navController.popBackStack() }
            )
        }

        // 設定画面
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}