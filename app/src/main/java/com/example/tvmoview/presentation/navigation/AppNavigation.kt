package com.example.tvmoview.presentation.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tvmoview.presentation.screens.*
import com.example.tvmoview.presentation.viewmodels.MediaBrowserViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val owner = LocalContext.current as ViewModelStoreOwner
    val sharedViewModel: MediaBrowserViewModel = viewModel(viewModelStoreOwner = owner)

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        // ホーム画面
        composable("home") {
            ModernMediaBrowser(
                viewModel = sharedViewModel,
                folderId = null,
                onMediaSelected = { mediaItem ->
                    if (mediaItem.isVideo) {
                        sharedViewModel.setFullscreenTransition(true)
                        navController.navigate("player/${mediaItem.id}")
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

        // フォルダ画面
        composable(
            "folder/{folderId}",
            arguments = listOf(
                navArgument("folderId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getString("folderId") ?: ""

            ModernMediaBrowser(
                viewModel = sharedViewModel,
                folderId = folderId,
                onMediaSelected = { mediaItem ->
                    if (mediaItem.isVideo) {
                        sharedViewModel.setFullscreenTransition(true)
                        navController.navigate("player/${mediaItem.id}")
                    }
                },
                onFolderSelected = { childFolderId ->
                    navController.navigate("folder/$childFolderId")
                },
                onBackClick = {
                    navController.popBackStack()
                },
                onSettingsClick = {
                    navController.navigate("settings")
                }
            )
        }

        // ✅ 既存のHighQualityPlayerScreenを使用
        composable(
            "player/{itemId}",
            arguments = listOf(
                navArgument("itemId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
            HighQualityPlayerScreen(
                itemId = itemId,
                onBack = {
                    navController.popBackStack()
                },
                viewModel = sharedViewModel
            )
        }

        // 設定画面
        composable("settings") {
            SettingsScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }}