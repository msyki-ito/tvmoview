package com.example.tvmoview.data.repository

import kotlinx.coroutines.flow.first

class StartupSyncUseCase(
    private val repository: CacheAwareMediaRepository
) {
    suspend operator fun invoke() {
        // Warm up cache without forcing network fetch
        repository.getFolderItems(null, force = false).first()
    }
}
