# 📘 SYMBOLS.md (generated 2025-06-27)

## 
- AuthenticationManager

## 
- AuthToken
  - [P] accessToken
  - [P] expiresAt
  - [P] isExpired
  - [P] refreshToken

## 
- DeviceCodeResponse
  - [P] deviceCode
  - [P] expiresIn
  - [P] interval
  - [P] userCode
  - [P] verificationUri

## 
- TokenResponse
  - [F] clearAuthentication()
  - [F] getSavedToken()
  - [F] isAuthenticated()
  - [F] pollForToken()
  - [F] startAuthentication()
  - [F] startDeviceCodeFlow()
  - [P] accessToken
  - [P] attempts
  - [P] deviceCode
  - [P] error
  - [P] errorDescription
  - [P] errorJson
  - [P] expirationTime
  - [P] expiresAt
  - [P] expiresIn
  - [P] interval
  - [P] json
  - [P] maxAttempts
  - [P] refreshToken
  - [P] request
  - [P] requestBody
  - [P] requestUrl
  - [P] response
  - [P] responseBody
  - [P] token
  - [P] tokenResult
  - [P] userCode
  - [P] verificationUri

## com.example.tvmoview
- Authenticated

## com.example.tvmoview
- Checking

## com.example.tvmoview
- ExampleInstrumentedTest
  - [F] useAppContext()
  - [P] appContext

## com.example.tvmoview
- ExampleUnitTest
  - [F] addition_isCorrect()

## com.example.tvmoview
- MainActivity
  - [F] AuthenticationWrapper()
  - [P] authState
  - [P] code
  - [P] data

## com.example.tvmoview
- NotAuthenticated
  - [F] AppNavigation()
  - [P] downloadUrl
  - [P] encodedDownloadUrl
  - [P] encodedUrl
  - [P] folderId
  - [P] itemId
  - [P] navController

## com.example.tvmoview.data.model
- AuthToken
  - [P] accessToken
  - [P] expiresAt
  - [P] isExpired
  - [P] refreshToken

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
- Success

## com.example.tvmoview.data.repository
- MediaRepository
  - [F] getCurrentPath()
  - [F] getFolderItems()
  - [F] getRootItems()

## com.example.tvmoview.data.repository
- OneDriveRepository
  - [F] getCurrentPath()
  - [F] getDownloadUrl()
  - [F] getFolderItems()
  - [F] getRootItems()
  - [P] downloadUrl
  - [P] isFolder
  - [P] items
  - [P] itemsWithDownloadUrl
  - [P] json
  - [P] lastModified
  - [P] mimeType
  - [P] request
  - [P] response
  - [P] responseBody
  - [P] size
  - [P] token
  - [P] url

## com.example.tvmoview.domain.model
- MediaItem
  - [P] downloadUrl
  - [P] fileExtension
  - [P] formattedSize
  - [P] id
  - [P] isFolder
  - [P] isImage
  - [P] isVideo
  - [P] lastModified
  - [P] mimeType
  - [P] name
  - [P] size
  - [P] thumbnailUrl

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
  - [F] loadItems()
  - [F] refresh()
  - [F] setSortBy()
  - [F] toggleViewMode()
  - [P] currentFolderId
  - [P] currentItems
  - [P] currentPath
  - [P] isLoading
  - [P] items
  - [P] sortBy
  - [P] sortedItems
  - [P] testItems
  - [P] viewMode

## com.example.tvmoview.tv
- FireTVOptimizations
  - [F] initializeForFireTV()


