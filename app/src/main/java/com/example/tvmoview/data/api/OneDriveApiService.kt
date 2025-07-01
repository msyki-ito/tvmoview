package com.example.tvmoview.data.api

import com.example.tvmoview.data.model.OneDriveResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface OneDriveApiService {
    
    @GET("me/drive/root/children")
    suspend fun getRootItems(
        @Header("Authorization") authorization: String,
        @Query("\$select") select: String = "id,name,size,lastModifiedDateTime,file,folder,@microsoft.graph.downloadUrl,video"
    ): Response<OneDriveResponse>
    
    @GET("me/drive/items/{itemId}/children")
    suspend fun getFolderItems(
        @Header("Authorization") authorization: String,
        @Path("itemId") itemId: String,
        @Query("\$select") select: String = "id,name,size,lastModifiedDateTime,file,folder,@microsoft.graph.downloadUrl,video"
    ): Response<OneDriveResponse>

    @GET("me/drive/items/{itemId}")
    suspend fun getItem(
        @Header("Authorization") authorization: String,
        @Path("itemId") itemId: String,
        @Query("\$select") select: String = "id,name,@microsoft.graph.downloadUrl"
    ): Response<com.example.tvmoview.data.model.OneDriveItem>

}