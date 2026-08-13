package com.worddeck.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.worddeck.R
import com.worddeck.common.AppError
import com.worddeck.domain.model.User

@Composable
fun HomeScreen(
    user: User,
    isLoggingOut: Boolean,
    error: AppError?,
    onLogout: () -> Unit,
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
            text = stringResource(R.string.welcome_user, user.displayName.value),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = user.email.value,
            style = MaterialTheme.typography.bodyLarge,
        )
        error?.let {
            Text(
                text = stringResource(R.string.logout_error),
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(
            onClick = onLogout,
            enabled = !isLoggingOut,
            modifier = Modifier.padding(top = 16.dp),
        ) {
            if (isLoggingOut) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text(stringResource(R.string.logout_action))
            }
        }
    }
}
