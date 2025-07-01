package com.example.tvmoview.data.model

import com.google.gson.annotations.SerializedName

data class OneDriveResponse(
    @SerializedName("value") val items: List<OneDriveItem>
)

data class OneDriveItem(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("size") val size: Long? = null,
    @SerializedName("lastModifiedDateTime") val lastModifiedDateTime: String,
    @SerializedName("file") val file: OneDriveFile? = null,
    @SerializedName("folder") val folder: OneDriveFolder? = null,
    @SerializedName("@microsoft.graph.downloadUrl") val downloadUrl: String? = null,
    @SerializedName("video") val video: OneDriveVideoInfo? = null
)

data class OneDriveVideoInfo(
    @SerializedName("duration") val duration: Long? = null
)

data class OneDriveFile(
    @SerializedName("mimeType") val mimeType: String
)

data class OneDriveFolder(
    @SerializedName("childCount") val childCount: Int? = null
)

data class AuthToken(
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresAt: Long = System.currentTimeMillis() + 3600000
) {
    val isExpired: Boolean
        get() = System.currentTimeMillis() > expiresAt
}

sealed class OneDriveResult<T> {
    data class Success<T>(val data: T) : OneDriveResult<T>()
    data class Error<T>(val exception: Throwable) : OneDriveResult<T>()
}