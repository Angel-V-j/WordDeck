package com.worddeck.feature.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.worddeck.R
import com.worddeck.common.AppResult
import com.worddeck.common.Timestamp
import com.worddeck.domain.model.ReviewEvent
import com.worddeck.ui.components.BackButton
import com.worddeck.ui.components.EmptyState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ReviewHistoryScreen(
    history: AppResult<List<ReviewEvent>>?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BackButton(onClick = onBack)
        Text(
            text = stringResource(R.string.review_history_title),
            style = MaterialTheme.typography.headlineMedium,
        )

        when (history) {
            null -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            is AppResult.Failure -> Text(
                text = stringResource(R.string.review_history_error),
                color = MaterialTheme.colorScheme.error,
            )
            is AppResult.Success -> if (history.value.isEmpty()) {
                EmptyState(stringResource(R.string.review_history_empty))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(history.value, key = { it.id.value }) { event ->
                        ReviewHistoryItem(event)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewHistoryItem(event: ReviewEvent) {
    Card(modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = qualityLabel(event.quality.value),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(formatReviewTime(event.reviewedAt), color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun qualityLabel(quality: Int): String = when (quality) {
    0 -> stringResource(R.string.rating_again)
    3 -> stringResource(R.string.rating_hard)
    4 -> stringResource(R.string.rating_good)
    5 -> stringResource(R.string.rating_easy)
    else -> stringResource(R.string.review_quality, quality)
}

private fun formatReviewTime(timestamp: Timestamp): String = REVIEW_TIME_FORMATTER.format(
    Instant.ofEpochMilli(timestamp.epochMilliseconds),
)

private val REVIEW_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter
    .ofPattern("yyyy-MM-dd HH:mm")
    .withZone(ZoneId.systemDefault())
