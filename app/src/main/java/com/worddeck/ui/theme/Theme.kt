package com.worddeck.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColorScheme = lightColorScheme(
    primary = WordDeckLightPrimary, onPrimary = Color.White,
    primaryContainer = Color(0xFFC5EBDD), onPrimaryContainer = Color(0xFF073C30),
    secondary = WordDeckLightSecondary, onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8EAF2), onSecondaryContainer = Color(0xFF173F50),
    tertiary = Color(0xFF7A5A25), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE6B4), onTertiaryContainer = Color(0xFF49320A),
    background = WordDeckLightBackground, onBackground = Color(0xFF1B2B25),
    surface = WordDeckLightBackground, onSurface = Color(0xFF1B2B25),
    surfaceVariant = Color(0xFFE2EAE4), onSurfaceVariant = Color(0xFF465B51),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFEEF3EE),
    surfaceContainer = Color(0xFFE8F0EA), surfaceContainerHigh = Color(0xFFE2EBE5),
    surfaceContainerHighest = Color(0xFFDAE6DE),
    outline = Color(0xFF708479), outlineVariant = Color(0xFFC6D5CC),
    error = Color(0xFFAD3538), onError = Color.White,
    errorContainer = Color(0xFFFFDAD8), onErrorContainer = Color(0xFF690D19),
)

private val DarkColorScheme = darkColorScheme(
    primary = WordDeckDarkPrimary, onPrimary = Color(0xFF00382B),
    primaryContainer = Color(0xFF205143), onPrimaryContainer = Color(0xFFC5EBDD),
    secondary = WordDeckDarkSecondary, onSecondary = Color(0xFF163744),
    secondaryContainer = Color(0xFF304E5C), onSecondaryContainer = Color(0xFFD8EAF2),
    tertiary = Color(0xFFE9C381), onTertiary = Color(0xFF422D08),
    tertiaryContainer = Color(0xFF604619), onTertiaryContainer = Color(0xFFFFE6B4),
    background = WordDeckDarkBackground, onBackground = Color(0xFFE1EBE4),
    surface = WordDeckDarkBackground, onSurface = Color(0xFFE1EBE4),
    surfaceVariant = Color(0xFF35463D), onSurfaceVariant = Color(0xFFBBCFC1),
    surfaceContainerLowest = Color(0xFF0B1410), surfaceContainerLow = Color(0xFF18241E),
    surfaceContainer = Color(0xFF1D2B23), surfaceContainerHigh = Color(0xFF26352D),
    surfaceContainerHighest = Color(0xFF304037),
    outline = Color(0xFF879E90), outlineVariant = Color(0xFF3E5447),
    error = Color(0xFFFFB3B0), onError = Color(0xFF680A18),
    errorContainer = Color(0xFF85212A), onErrorContainer = Color(0xFFFFDAD8),
)

private val WordDeckTypography = Typography(
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold,
        fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = (-0.6).sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp),
)

@Composable
fun WordDeckTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme,
        typography = WordDeckTypography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(28.dp),
        ),
        content = content,
    )
}
