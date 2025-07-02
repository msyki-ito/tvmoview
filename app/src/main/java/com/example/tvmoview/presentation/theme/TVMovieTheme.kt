package com.example.tvmoview.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Hulu風ダークテーマ
private val HuluDarkColorScheme = darkColorScheme(
    background = Color(0xFF0B0C0F),
    surface = Color(0xFF1A1C22),
    primary = Color(0xFF1CE783),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFB0B0B0),
    surfaceVariant = Color(0xFF1A1C22)
)

// ライトテーマのカラーパレット
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0288D1),
    secondary = Color(0xFF388E3C),
    tertiary = Color(0xFFFF8F00),
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

@Composable
fun TVMovieTheme(
    darkTheme: Boolean = true, // Android TVは通常ダークテーマ
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        HuluDarkColorScheme
        } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
