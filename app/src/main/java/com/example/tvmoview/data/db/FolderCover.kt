package com.example.tvmoview.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folder_covers")
data class FolderCover(
    @PrimaryKey val folderId: String,
    val itemId: String
)
