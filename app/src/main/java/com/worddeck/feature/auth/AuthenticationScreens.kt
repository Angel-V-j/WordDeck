package com.worddeck.feature.auth

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.worddeck.R

@Composable
fun LoginScreen(modifier: Modifier = Modifier) {
    AuthenticationPlaceholder(
        titleRes = R.string.login_title,
        modifier = modifier,
    )
}

@Composable
fun RegisterScreen(modifier: Modifier = Modifier) {
    AuthenticationPlaceholder(
        titleRes = R.string.register_title,
        modifier = modifier,
    )
}

@Composable
private fun AuthenticationPlaceholder(
    @StringRes titleRes: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.authentication_placeholder_message),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
