package com.piyush.livetranslate.core.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.piyush.livetranslate.core.model.ThemeMode

private val Indigo = Color(0xFF635BFF)
private val Cyan = Color(0xFF25C7D9)
private val DarkScheme = darkColorScheme(
    primary = Color(0xFFAFA9FF),
    onPrimary = Color(0xFF221B76),
    secondary = Color(0xFF75DCE8),
    tertiary = Color(0xFFFFB3C8),
    background = Color(0xFF0D0F17),
    surface = Color(0xFF151824),
    surfaceVariant = Color(0xFF202432),
)
private val LightScheme = lightColorScheme(
    primary = Indigo,
    secondary = Color(0xFF006975),
    tertiary = Color(0xFF9B405F),
    background = Color(0xFFF8F8FF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFECECF7),
)

@Composable
fun LiveTranslateTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        dark -> DarkScheme
        else -> LightScheme
    }
    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography.copy(
            displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 36.sp),
            headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 26.sp),
            titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
        ),
        content = content,
    )
}

val BrandGradient = listOf(Indigo, Cyan)
