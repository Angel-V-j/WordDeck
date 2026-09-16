package com.worddeck.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import com.worddeck.R
import com.worddeck.ui.theme.WordDeckLightPrimary

internal enum class MainTab(val route: String, val label: Int, val icon: Int) {
    HOME(AppDestination.HOME, R.string.home_action, R.drawable.ic_home),
    STATISTICS(AppDestination.STATISTICS, R.string.statistics_action, R.drawable.ic_statistics),
    PROFILE(AppDestination.PROFILE, R.string.profile_title, R.drawable.ic_profile),
}

@Composable
internal fun WordDeckNavigationBar(currentRoute: String?, onSelect: (MainTab) -> Unit) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            MainTab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = currentRoute == tab.route,
                    onClick = { onSelect(tab) },
                    icon = { Icon(painterResource(tab.icon), contentDescription = null, modifier = Modifier.size(24.dp)) },
                    label = {
                        BasicText(
                            text = stringResource(tab.label),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = LocalContentColor.current, textAlign = TextAlign.Center,
                            ),
                            maxLines = 1,
                            autoSize = TextAutoSize.StepBased(minFontSize = 10.sp,
                                maxFontSize = MaterialTheme.typography.labelMedium.fontSize),
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = WordDeckLightPrimary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}
