package com.example.tvmoview.presentation.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.example.tvmoview.MainActivity

@Composable
fun AuthenticationWrapper(content: @Composable () -> Unit) {
    var authState by remember { mutableStateOf<AuthState>(AuthState.Checking) }

    LaunchedEffect(Unit) {
        authState = if (MainActivity.authManager.isAuthenticated()) {
            AuthState.Authenticated
        } else {
            AuthState.NotAuthenticated
        }
    }

    when (authState) {
        AuthState.Checking -> Text("起動中...")
        AuthState.Authenticated -> content()
        AuthState.NotAuthenticated -> LoginScreen(
            onLoginSuccess = { authState = AuthState.Authenticated },
            onUseTestData = { authState = AuthState.Authenticated }
        )
    }
}

sealed class AuthState {
    object Checking : AuthState()
    object Authenticated : AuthState()
    object NotAuthenticated : AuthState()
}