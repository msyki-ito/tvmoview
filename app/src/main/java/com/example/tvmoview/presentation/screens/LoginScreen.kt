package com.example.tvmoview.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tvmoview.MainActivity
import com.example.tvmoview.data.auth.AuthenticationManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit = {},
    onUseTestData: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var authState by remember { mutableStateOf<DeviceAuthState>(DeviceAuthState.Ready) }
    var deviceCodeResponse by remember { mutableStateOf<AuthenticationManager.DeviceCodeResponse?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            when (authState) {
                DeviceAuthState.Ready -> {
                    ReadyToLoginContent(
                        onStartLogin = {
                            authState = DeviceAuthState.GettingCode
                            scope.launch {
                                try {
                                    val response = MainActivity.authManager.startDeviceCodeFlow()
                                    deviceCodeResponse = response
                                    authState = DeviceAuthState.ShowingCode

                                    // バックグラウンドでトークン取得開始
                                    launch {
                                        try {
                                            MainActivity.authManager.pollForToken(response.deviceCode, response.interval)
                                            authState = DeviceAuthState.Success
                                            onLoginSuccess()
                                        } catch (e: Exception) {
                                            errorMessage = e.message
                                            authState = DeviceAuthState.Error
                                        }
                                    }
                                } catch (e: Exception) {
                                    errorMessage = e.message
                                    authState = DeviceAuthState.Error
                                }
                            }
                        },
                        onUseTestData = onUseTestData
                    )
                }

                DeviceAuthState.GettingCode -> {
                    GettingCodeContent()
                }

                DeviceAuthState.ShowingCode -> {
                    deviceCodeResponse?.let { response ->
                        ShowingCodeContent(
                            userCode = response.userCode,
                            verificationUri = response.verificationUri,
                            onCancel = {
                                authState = DeviceAuthState.Ready
                                deviceCodeResponse = null
                            }
                        )
                    }
                }

                DeviceAuthState.Success -> {
                    SuccessContent()
                }

                DeviceAuthState.Error -> {
                    ErrorContent(
                        message = errorMessage ?: "不明なエラー",
                        onRetry = {
                            authState = DeviceAuthState.Ready
                            errorMessage = null
                            deviceCodeResponse = null
                        }
                    )
                }
            }
        }
    }
}
}

@Composable
fun ReadyToLoginContent(
    onStartLogin: () -> Unit,
    onUseTestData: () -> Unit
) {
    Icon(
        imageVector = Icons.Default.CloudSync,
        contentDescription = "OneDrive",
        modifier = Modifier.size(80.dp),
        tint = MaterialTheme.colorScheme.primary
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "TV Movie Viewer",
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Android TV版 OneDrive連携",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = onStartLogin,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Icon(
            imageVector = Icons.Default.PhoneAndroid,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("OneDriveにログイン", fontSize = 18.sp)
    }

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedButton(
        onClick = onUseTestData,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("テストデータで試す")
    }
}

@Composable
fun GettingCodeContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            strokeWidth = 6.dp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "認証コードを取得中...",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ShowingCodeContent(
    userCode: String,
    verificationUri: String,
    onCancel: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "📱 PC・スマホで認証",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 認証コードを大きく表示
        Card(
            modifier = Modifier.padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "認証コード",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = userCode,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // URLを確実に表示（デバッグ）
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🌐 アクセスするURL:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                // デバッグ: URLが空の場合のフォールバック
                val displayUrl = if (verificationUri.isNotEmpty() && verificationUri != "NOT_FOUND") {
                    verificationUri
                } else {
                    "https://microsoft.com/devicelogin"
                }

                Text(
                    text = displayUrl,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )

                // デバッグ情報
                Text(
                    text = "Debug: '$verificationUri'",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "📝 手順:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("1. PC・スマホのブラウザで上記URLを開く")
                Text("2. 認証コード「$userCode」を入力")
                Text("3. OneDriveアカウントでログイン")
                Text("4. 認証完了まで待機")

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "💡 一般的なURL: https://microsoft.com/devicelogin",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("認証完了を待機中...")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(onClick = onCancel) {
            Text("キャンセル")
        }
    }
}

@Composable
fun SuccessContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🎉",
            fontSize = 64.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "認証完了！",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "OneDriveファイルを読み込み中...",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "❌ エラー",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRetry) {
            Text("再試行")
        }
    }
}

sealed class DeviceAuthState {
    object Ready : DeviceAuthState()
    object GettingCode : DeviceAuthState()
    object ShowingCode : DeviceAuthState()
    object Success : DeviceAuthState()
    object Error : DeviceAuthState()
}
