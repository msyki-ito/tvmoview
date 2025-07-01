package com.example.tvmoview.data.db

import android.content.Context
import androidx.room.Room

object MediaDatabaseProvider {
    lateinit var database: MediaDatabase
        private set

    fun init(context: Context) {
        database = Room.databaseBuilder(
            context,
            MediaDatabase::class.java,
            "media_cache.db"
        )
            .fallbackToDestructiveMigration()
            .build()
        val path = context.getDatabasePath("media_cache.db").absolutePath
        android.util.Log.d("MediaDatabase", "📂 cache DB initialized at $path")
    }
}
