                                    package com.example.tvmoview.data.auth

                                    import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
                                    import android.util.Log
                                    import kotlinx.coroutines.*
                                    import okhttp3.*
                                    import okhttp3.MediaType.Companion.toMediaType
                                    import okhttp3.RequestBody.Companion.toRequestBody
                                    import org.json.JSONObject
                                    import java.io.IOException
                                    import java.util.*

private const val REFRESH_THRESHOLD_MS = 5 * 60 * 1000L

class AuthenticationManager(private val context: Context) {

                                        private val clientId = "c7981c06-6cf2-4c9c-b98e-c51c83073972"
                                        private val tenantId = "consumers"
                                        private val scope = "https://graph.microsoft.com/Files.Read offline_access"

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "auth_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
                                        private val httpClient = OkHttpClient()

                                        // Device Code Flow用のデータクラス
                                        data class DeviceCodeResponse(
                                            val deviceCode: String,
                                            val userCode: String,
                                            val verificationUri: String,
                                            val expiresIn: Int,
                                            val interval: Int
                                        )

                                        data class TokenResponse(
                                            val accessToken: String,
                                            val refreshToken: String?,
                                            val expiresIn: Int
                                        )

                                        /**
                                         * Android TV用のDevice Code Flow開始
                                         * @return DeviceCodeResponse 認証コード情報
                                         */
                                        suspend fun startDeviceCodeFlow(): DeviceCodeResponse = withContext(Dispatchers.IO) {
                                            Log.d("AuthenticationManager", "🔐 Device Code Flow開始")
                                            Log.d("AuthenticationManager", "Client ID: $clientId")
                                            Log.d("AuthenticationManager", "Scope: $scope")
                                            Log.d("AuthenticationManager", "Tenant ID: $tenantId")
                                            Log.d("AuthenticationManager", "🚨 変更確認: tenantId = '$tenantId' (consumers であるべき)")

                                            val requestBody = FormBody.Builder()
                                                .add("client_id", clientId)
                                                .add("scope", scope)
                                                .build()

                                            val requestUrl = "https://login.microsoftonline.com/$tenantId/oauth2/v2.0/devicecode"
                                            Log.d("AuthenticationManager", "📡 Request URL: $requestUrl")
                                            Log.d("AuthenticationManager", "🚨 URL確認: consumersが含まれているか? ${requestUrl.contains("consumers")}")

                                            val request = Request.Builder()
                                                .url(requestUrl)
                                                .post(requestBody)
                                                .build()

                                            val response = httpClient.newCall(request).execute()
                                            val responseBody = response.body?.string()

                                            Log.d("AuthenticationManager", "📡 Response Code: ${response.code}")
                                            Log.d("AuthenticationManager", "📡 Response Headers: ${response.headers}")
                                            Log.d("AuthenticationManager", "📡 Device Code APIレスポンス全体: $responseBody")

                                            if (!response.isSuccessful || responseBody == null) {
                                                Log.e("AuthenticationManager", "❌ Device code request failed: ${response.code}")
                                                Log.e("AuthenticationManager", "❌ Error Body: $responseBody")

                                                // エラーの詳細を解析
                                                if (responseBody != null) {
                                                    try {
                                                        val errorJson = JSONObject(responseBody)
                                                        val error = errorJson.optString("error")
                                                        val errorDescription = errorJson.optString("error_description")
                                                        Log.e("AuthenticationManager", "❌ Error: $error")
                                                        Log.e("AuthenticationManager", "❌ Error Description: $errorDescription")
                                                        throw Exception("Device Code Error: $error - $errorDescription")
                                                    } catch (e: Exception) {
                                                        throw Exception("Device code request failed: ${response.code} - $responseBody")
                                                    }
                                                }
                                                throw Exception("Device code request failed: ${response.code}")
                                            }

                                            val json = JSONObject(responseBody)

                                            // 個別にフィールドを取得してログ出力
                                            val deviceCode = json.optString("device_code", "NOT_FOUND")
                                            val userCode = json.optString("user_code", "NOT_FOUND")
                                            val verificationUri = json.optString("verification_uri", "NOT_FOUND")
                                            val expiresIn = json.optInt("expires_in", 0)
                                            val interval = json.optInt("interval", 5)

                                            Log.d("AuthenticationManager", "✅ Device Code取得詳細:")
                                            Log.d("AuthenticationManager", "  Device Code: $deviceCode")
                                            Log.d("AuthenticationManager", "  User Code: $userCode")
                                            Log.d("AuthenticationManager", "  Verification URI: $verificationUri")
                                            Log.d("AuthenticationManager", "  Expires In: $expiresIn")
                                            Log.d("AuthenticationManager", "  Interval: $interval")

                                            if (verificationUri == "NOT_FOUND" || userCode == "NOT_FOUND") {
                                                Log.e("AuthenticationManager", "❌ 必須フィールドが見つかりません")
                                                throw Exception("認証情報の取得に失敗しました")
                                            }

                                            DeviceCodeResponse(
                                                deviceCode = deviceCode,
                                                userCode = userCode,
                                                verificationUri = verificationUri,
                                                expiresIn = expiresIn,
                                                interval = interval
                                            )
                                        }

                                        /**
                                         * Device Code Flowでのトークン取得（ポーリング）
                                         */
                                        suspend fun pollForToken(deviceCode: String, interval: Int): TokenResponse {
                                            return withContext(Dispatchers.IO) {
                                                Log.d("AuthenticationManager", "⏰ トークン取得ポーリング開始")
                                                Log.d("AuthenticationManager", "Device Code: $deviceCode")
                                                Log.d("AuthenticationManager", "Interval: $interval")

                                                var attempts = 0
                                                val maxAttempts = 60 // 5分間のタイムアウト（短縮してテスト）

                                                while (attempts < maxAttempts) {
                                                    attempts++
                                                    Log.d("AuthenticationManager", "🔄 ポーリング試行 $attempts/$maxAttempts")

                                                    delay(interval * 1000L)

                                                    val requestBody = FormBody.Builder()
                                                        .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                                                        .add("client_id", clientId)
                                                        .add("device_code", deviceCode)
                                                        .build()

                                                    val request = Request.Builder()
                                                        .url("https://login.microsoftonline.com/$tenantId/oauth2/v2.0/token")
                                                        .post(requestBody)
                                                        .build()

                                                    try {
                                                        Log.d("AuthenticationManager", "📡 トークンAPI呼び出し中...")
                                                        val response = httpClient.newCall(request).execute()
                                                        val responseBody = response.body?.string()

                                                        Log.d("AuthenticationManager", "📡 Response Code: ${response.code}")
                                                        Log.d("AuthenticationManager", "📡 Response Body: $responseBody")

                                                        if (response.isSuccessful && responseBody != null) {
                                                            val json = JSONObject(responseBody)

                                                            if (json.has("access_token")) {
                                                                Log.d("AuthenticationManager", "🎉 アクセストークン取得成功！")

                                                                // TokenResponse作成
                                                                val tokenResult = TokenResponse(
                                                                    accessToken = json.getString("access_token"),
                                                                    refreshToken = json.optString("refresh_token"),
                                                                    expiresIn = json.getInt("expires_in")
                                                                )

                                                                // 保存処理（別行で実行）
                                                                saveTokenResponse(tokenResult)
                                                                Log.d("AuthenticationManager", "✅ トークン保存完了")

                                                                // 成功時は即座にreturn
                                                                return@withContext tokenResult
                                                            }
                                                        }

                                                        // エラーハンドリング
                                                        if (responseBody != null) {
                                                            val json = JSONObject(responseBody)
                                                            val error = json.optString("error")

                                                            Log.d("AuthenticationManager", "📄 Error: $error")

                                                            if (error == "authorization_pending") {
                                                                Log.d("AuthenticationManager", "⏳ ユーザー認証待機中... ($attempts/$maxAttempts)")
                                                                continue
                                                            } else if (error == "authorization_declined") {
                                                                Log.e("AuthenticationManager", "❌ ユーザーが認証を拒否")
                                                                throw Exception("ユーザーが認証を拒否しました")
                                                            } else if (error == "expired_token") {
                                                                Log.e("AuthenticationManager", "❌ 認証コードが期限切れ")
                                                                throw Exception("認証コードが期限切れです")
                                                            } else if (error.isNotEmpty()) {
                                                                Log.e("AuthenticationManager", "❌ 認証エラー: $error")
                                                                throw Exception("認証エラー: $error")
                                                            }
                                                        }

                                                    } catch (e: Exception) {
                                                        Log.e("AuthenticationManager", "🚨 ネットワークエラー: ${e.message}")
                                                        if (e.message?.contains("認証") == true || e.message?.contains("期限") == true) {
                                                            throw e
                                                        }
                                                        // ネットワークエラーの場合は続行
                                                    }
                                                }

                                                // タイムアウト
                                                Log.e("AuthenticationManager", "⏰ 認証タイムアウト")
                                                throw Exception("認証がタイムアウトしました。再度お試しください。")
                                            }
                                        }

    private fun saveTokenResponse(tokenResponse: TokenResponse) {
        val expirationTime = System.currentTimeMillis() + (tokenResponse.expiresIn * 1000L)

        prefs.edit()
            .putString("access_token", tokenResponse.accessToken)
            .putString("refresh_token", tokenResponse.refreshToken)
            .putLong("expires_at", expirationTime)
            .apply()
    }

    private suspend fun refreshAccessToken(refreshToken: String): TokenResponse? = withContext(Dispatchers.IO) {
        val body = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("client_id", clientId)
            .add("scope", scope)
            .add("refresh_token", refreshToken)
            .build()

        val request = Request.Builder()
            .url("https://login.microsoftonline.com/$tenantId/oauth2/v2.0/token")
            .post(body)
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()
            if (response.isSuccessful && responseBody != null) {
                val json = JSONObject(responseBody)
                return@withContext TokenResponse(
                    accessToken = json.getString("access_token"),
                    refreshToken = json.optString("refresh_token"),
                    expiresIn = json.getInt("expires_in")
                )
            } else {
                Log.e("AuthenticationManager", "refresh failed: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e("AuthenticationManager", "refresh error", e)
        }
        null
    }

    fun getSavedToken(): AuthToken? {
        val accessToken = prefs.getString("access_token", null) ?: return null
        val refreshToken = prefs.getString("refresh_token", null)
        val expiresAt = prefs.getLong("expires_at", 0)

        return AuthToken(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = expiresAt
        )
    }

    suspend fun getValidToken(): AuthToken? {
        val token = getSavedToken() ?: return null
        if (token.shouldRefresh && token.refreshToken != null) {
            val refreshed = refreshAccessToken(token.refreshToken)
            if (refreshed != null) {
                saveTokenResponse(refreshed)
                return getSavedToken()
            }
        }
        return if (token.isExpired) null else token
    }

    fun isAuthenticated(): Boolean {
        return runBlocking { getValidToken() } != null
    }

                                        fun clearAuthentication() {
                                            prefs.edit().clear().apply()
                                            Log.d("AuthenticationManager", "🔓 認証情報をクリア")
                                        }

                                        // 旧メソッド（互換性のため残す）
                                        fun startAuthentication(): android.content.Intent {
                                            throw UnsupportedOperationException("Android TVではDevice Code Flowを使用してください")
                                        }
                                    }

data class AuthToken(
    val accessToken: String,
    val refreshToken: String?,
    val expiresAt: Long
) {
    val isExpired: Boolean
        get() = System.currentTimeMillis() >= expiresAt
    val shouldRefresh: Boolean
        get() = System.currentTimeMillis() >= expiresAt - REFRESH_THRESHOLD_MS
}
