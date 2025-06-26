# USAGE MAP

- `CacheAwareMediaRepository` synchronises OneDrive API data with Room DB.
- `AppDatabase` stores `CachedMediaItemEntity` and `FolderSyncStatusEntity`.
- ViewModel observes cached data via repository Flow.
- ImageLoader is configured with disk cache max 10MB.
- `StartupSyncUseCase` warms the cache at launch without WorkManager.
- Cache is trimmed to 1000 items total and 100 per folder.
