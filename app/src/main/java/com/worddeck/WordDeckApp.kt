package com.worddeck

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.worddeck.navigation.AppNavigation
import com.worddeck.ui.theme.WordDeckTheme

@Composable
fun WordDeckApp() {
    WordDeckTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AppNavigation(modifier = Modifier.fillMaxSize())
        }
    }
}
