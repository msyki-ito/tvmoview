package com.example.tvmoview.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ダークテーマのカラーパレット
object HuluColors {
    val Background = Color(0xFF0B0C0F)
    val Surface = Color(0xFF1A1C22)
    val CardBackground = Color(0xFF16181D)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFB0B0B0)
    val TextTertiary = Color(0xFF808080)
    val Divider = Color(0xFF2A2C32)
}

private val HuluDarkColorScheme = darkColorScheme(
    background = HuluColors.Background,
    surface = HuluColors.Surface,
    onBackground = HuluColors.TextPrimary,
    onSurface = HuluColors.TextSecondary,
    surfaceVariant = HuluColors.CardBackground
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
    darkTheme: Boolean = true,
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
