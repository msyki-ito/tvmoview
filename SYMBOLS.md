# 📘 SYMBOLS.md (generated 2025-07-02)

## 
- AuthenticationManager

## 
- AuthToken
  - [P] accessToken: String,
  - [P] expiresAt: Long
  - [P] isExpired: Boolean
  - [P] refreshToken: String?,
  - [P] shouldRefresh: Boolean

## 
- DeviceCodeResponse
  - [P] deviceCode: String,
  - [P] expiresIn: Int,
  - [P] interval: Int
  - [P] userCode: String,
  - [P] verificationUri: String,

## 
- TokenResponse
  - [F] clearAuthentication()
  - [F] getSavedToken(): AuthToken?
  - [F] getValidToken(): AuthToken?
  - [F] isAuthenticated(): Boolean
  - [F] pollForToken(deviceCode: String, interval: Int): TokenResponse
  - [F] startAuthentication(): android.content.Intent
  - [F] startDeviceCodeFlow(): DeviceCodeResponse
  - [P] accessToken: String,
  - [P] expiresIn: Int
  - [P] refreshToken: String?,

## com.example.tvmoview
- Authenticated

## com.example.tvmoview
- Checking

## com.example.tvmoview
- ExampleInstrumentedTest
  - [F] useAppContext()

## com.example.tvmoview
- ExampleUnitTest
  - [F] addition_isCorrect()

## com.example.tvmoview
- MainActivity
  - [F] AuthenticationWrapper()

## com.example.tvmoview
- NotAuthenticated
  - [F] AppNavigation()

## com.example.tvmoview.data.db
- CachedMediaItem
  - [P] downloadUrl: String?,
  - [P] duration: Long
  - [P] isFolder: Boolean,
  - [P] lastAccessedAt: Long
  - [P] lastModified: Long,
  - [P] mimeType: String?,
  - [P] name: String,
  - [P] parentId: String?,
  - [P] size: Long,
  - [P] thumbnailUrl: String?,

## com.example.tvmoview.data.db
- FolderSyncStatus
  - [P] lastSyncAt: Long

## com.example.tvmoview.data.db
- MediaDatabaseProvider
  - [F] init(context: Context)

## com.example.tvmoview.data.model
- AuthToken
  - [P] accessToken: String,
  - [P] expiresAt: Long
  - [P] isExpired: Boolean
  - [P] refreshToken: String?

## com.example.tvmoview.data.model
- Error

## com.example.tvmoview.data.model
- OneDriveFile

## com.example.tvmoview.data.model
- OneDriveFolder

## com.example.tvmoview.data.model
- OneDriveItem

## com.example.tvmoview.data.model
- OneDriveResponse

## com.example.tvmoview.data.model
- OneDriveVideo

## com.example.tvmoview.data.model
- Success

## com.example.tvmoview.data.prefs
- UserPreferences
  - [F] clearResumePosition(id: String)
  - [F] getResumePosition(id: String): Long
  - [F] init(context: Context)
  - [F] setResumePosition(id: String, position: Long)
  - [P] sortBy: String
  - [P] sortOrder: String
  - [P] tileColumns: Int

## com.example.tvmoview.data.repository
- MediaRepository
  - [F] getCurrentPath(folderId: String?): String
  - [F] getFolderItems(folderId: String): List<MediaItem>
  - [F] getRootItems(): List<MediaItem>

## com.example.tvmoview.data.repository
- OneDriveRepository
  - [F] getCachedItems(folderId: String?): List<MediaItem>
  - [F] getCurrentPath(folderId: String?): String
  - [F] getDownloadUrl(itemId: String): String?
  - [F] getFolderItems(folderId: String? = null, force: Boolean = false): Flow<List<MediaItem>>

## com.example.tvmoview.domain.model
- MediaItem
  - [P] downloadUrl: String?
  - [P] duration: Long
  - [P] fileExtension: String
  - [P] formattedSize: String
  - [P] id: String,
  - [P] isFolder: Boolean
  - [P] isImage: Boolean
  - [P] isVideo: Boolean
  - [P] lastModified: Date
  - [P] mimeType: String?
  - [P] name: String,
  - [P] size: Long
  - [P] thumbnailUrl: String?

## com.example.tvmoview.presentation.screens
- Authenticated

## com.example.tvmoview.presentation.screens
- Checking

## com.example.tvmoview.presentation.screens
- Error

## com.example.tvmoview.presentation.screens
- GettingCode

## com.example.tvmoview.presentation.screens
- NotAuthenticated

## com.example.tvmoview.presentation.screens
- Ready

## com.example.tvmoview.presentation.screens
- ShowingCode

## com.example.tvmoview.presentation.screens
- Success

## com.example.tvmoview.presentation.viewmodels
- MediaBrowserViewModel
  - [F] cycleTileColumns()
  - [F] loadItems(folderId: String? = null, force: Boolean = false)
  - [F] refresh()
  - [F] saveScrollPosition(index: Int)
  - [F] setSortBy(sortBy: SortBy)
  - [F] setSortOrder(order: SortOrder)
  - [F] toggleViewMode()
  - [P] currentFolderId: StateFlow<String?>
  - [P] currentPath: StateFlow<String>
  - [P] isLoading: StateFlow<Boolean>
  - [P] items: StateFlow<List<MediaItem>>
  - [P] lastIndex: StateFlow<Int>
  - [P] sortBy: StateFlow<SortBy>
  - [P] sortOrder: StateFlow<SortOrder>
  - [P] tileColumns: StateFlow<Int>
  - [P] viewMode: StateFlow<ViewMode>

## com.example.tvmoview.tv
- FireTVOptimizations
  - [F] initializeForFireTV(activity: Activity)


