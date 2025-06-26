package com.example.tvmoview.presentation.util

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache

object ImageLoaderProvider {
    fun create(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("images"))
                    .maxSizeBytes(10L * 1024 * 1024)
                    .build()
            }
            .memoryCache { MemoryCache.Builder(context).maxSizeBytes(0).build() }
            .build()
    }
}
