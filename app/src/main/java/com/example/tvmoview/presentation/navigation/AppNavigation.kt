package com.example.tvmoview.presentation.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tvmoview.presentation.screens.*
import java.net.URLDecoder
import java.net.URLEncoder

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        // ホーム画面
        composable("home") {
            ModernMediaBrowser(
                folderId = null,
                onMediaSelected = { mediaItem ->
                    if (mediaItem.isVideo) {
                        // 既存のHighQualityPlayerScreenを使用
                        val encodedId = safeUrlEncode(mediaItem.id)
                        val encodedUrl = safeUrlEncode(mediaItem.downloadUrl ?: "")
                        navController.navigate("player/$encodedId/$encodedUrl")
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
                onMediaSelected = { mediaItem ->
                    if (mediaItem.isVideo) {
                        val encodedId = safeUrlEncode(mediaItem.id)
                        val encodedUrl = safeUrlEncode(mediaItem.downloadUrl ?: "")
                        navController.navigate("player/$encodedId/$encodedUrl")
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
            "player/{itemId}/{downloadUrl}",
            arguments = listOf(
                navArgument("itemId") { type = NavType.StringType },
                navArgument("downloadUrl") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedItemId = backStackEntry.arguments?.getString("itemId") ?: ""
            val encodedDownloadUrl = backStackEntry.arguments?.getString("downloadUrl") ?: ""

            val itemId = safeUrlDecode(encodedItemId)
            val downloadUrl = safeUrlDecode(encodedDownloadUrl)

            HighQualityPlayerScreen(
                itemId = itemId,
                downloadUrl = downloadUrl,
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
    }
}

// URL安全エンコード/デコード
private fun safeUrlEncode(input: String): String {
    return try {
        URLEncoder.encode(input, "UTF-8")
    } catch (e: Exception) {
        input.replace("/", "%2F")
    }
}

private fun safeUrlDecode(encoded: String): String {
    return try {
        URLDecoder.decode(encoded, "UTF-8")
    } catch (e: Exception) {
        encoded.replace("%2F", "/")
    }
}