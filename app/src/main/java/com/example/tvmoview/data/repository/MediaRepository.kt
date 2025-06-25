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
                name = "📁 写真コレクション",
                size = 0,
                lastModified = Date(),
                isFolder = true
            ),
            MediaItem(
                id = "folder2", 
                name = "📁 動画ライブラリ",
                size = 0,
                lastModified = Date(),
                isFolder = true
            ),
            MediaItem(
                id = "folder3",
                name = "📁 ドキュメント",
                size = 0,
                lastModified = Date(),
                isFolder = true
            ),
            MediaItem(
                id = "video1",
                name = "🎬 家族の思い出.mp4",
                size = 52428800,
                lastModified = Date(System.currentTimeMillis() - 86400000),
                mimeType = "video/mp4"
            ),
            MediaItem(
                id = "video2",
                name = "🎬 旅行記録.mp4",
                size = 85214800,
                lastModified = Date(System.currentTimeMillis() - 172800000),
                mimeType = "video/mp4"
            ),
            MediaItem(
                id = "image1",
                name = "🖼️ 夕日の風景.jpg",
                size = 3145728,
                lastModified = Date(System.currentTimeMillis() - 259200000),
                mimeType = "image/jpeg"
            ),
            MediaItem(
                id = "image2",
                name = "🖼️ 花の写真.jpg",
                size = 2456789,
                lastModified = Date(System.currentTimeMillis() - 345600000),
                mimeType = "image/jpeg"
            )
        )
    }

    suspend fun getFolderItems(folderId: String): List<MediaItem> {
        delay(800)

        return when (folderId) {
            "folder1" -> {
                // 写真フォルダの内容
                (1..12).map { i ->
                    MediaItem(
                        id = "image_$i",
                        name = "📸 写真$i.jpg",
                        size = (1000000..5000000).random().toLong(),
                        lastModified = Date(System.currentTimeMillis() - i * 86400000),
                        mimeType = "image/jpeg"
                    )
                }
            }
            "folder2" -> {
                // 動画フォルダの内容
                (1..8).map { i ->
                    MediaItem(
                        id = "video_$i",
                        name = "🎥 動画$i.mp4",
                        size = (50000000..200000000).random().toLong(),
                        lastModified = Date(System.currentTimeMillis() - i * 86400000),
                        mimeType = "video/mp4"
                    )
                }
            }
            "folder3" -> {
                // ドキュメントフォルダの内容
                listOf(
                    MediaItem(
                        id = "doc1",
                        name = "📄 重要資料.pdf",
                        size = 1234567,
                        lastModified = Date(),
                        mimeType = "application/pdf"
                    ),
                    MediaItem(
                        id = "doc2",
                        name = "📄 プレゼン資料.pptx",
                        size = 2345678,
                        lastModified = Date(),
                        mimeType = "application/vnd.ms-powerpoint"
                    )
                )
            }
            else -> emptyList()
        }
    }

    fun getCurrentPath(folderId: String?): String {
        return when (folderId) {
            "folder1" -> "写真コレクション"
            "folder2" -> "動画ライブラリ"
            "folder3" -> "ドキュメント"
            else -> "OneDrive TV"
        }
    }
}
