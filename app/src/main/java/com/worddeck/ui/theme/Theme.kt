package com.worddeck.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = WordDeckLightPrimary,
    secondary = WordDeckLightSecondary,
    background = WordDeckLightBackground,
    surface = WordDeckLightBackground,
)

private val DarkColorScheme = darkColorScheme(
    primary = WordDeckDarkPrimary,
    secondary = WordDeckDarkSecondary,
    background = WordDeckDarkBackground,
    surface = WordDeckDarkBackground,
)

@Composable
fun WordDeckTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) {
            DarkColorScheme
        } else {
            LightColorScheme
        },
        content = content,
    )
}
