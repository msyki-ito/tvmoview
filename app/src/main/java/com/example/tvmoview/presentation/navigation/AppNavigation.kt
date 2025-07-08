package com.example.tvmoview.presentation.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tvmoview.presentation.screens.*
import com.example.tvmoview.presentation.viewmodels.SharedExoPlayerViewModel
import com.example.tvmoview.presentation.viewmodels.ViewMode

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val sharedPlayerViewModel: SharedExoPlayerViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        // ホーム画面
        composable("home") {
            ModernMediaBrowser(
                folderId = null,
                viewMode = ViewMode.HOME_VIDEO,
                sharedPlayerViewModel = sharedPlayerViewModel,
                onMediaSelected = { mediaItem ->
                    if (mediaItem.isVideo) {
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
                folderId = folderId,
                sharedPlayerViewModel = sharedPlayerViewModel,
                onMediaSelected = { mediaItem ->
                    if (mediaItem.isVideo) {
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
                sharedPlayerViewModel = sharedPlayerViewModel,
                onBack = {
                    navController.popBackStack()
                }
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