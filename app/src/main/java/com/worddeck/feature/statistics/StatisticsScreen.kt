package com.worddeck.feature.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.worddeck.R
import com.worddeck.common.OperationStatus
import com.worddeck.ui.components.BackButton
import com.worddeck.ui.components.EmptyState

@Composable
fun StatisticsScreen(
    title: String,
    uiState: StatisticsUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BackButton(onClick = onBack)
        Text(title, style = MaterialTheme.typography.headlineMedium)

        when (uiState.status) {
            OperationStatus.IDLE,
            OperationStatus.LOADING,
            -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            OperationStatus.ERROR -> Text(
                text = stringResource(R.string.statistics_error),
                color = MaterialTheme.colorScheme.error,
            )
            OperationStatus.SUCCESS -> StatisticsContent(uiState)
        }
    }
}

@Composable
private fun StatisticsContent(uiState: StatisticsUiState) {
    val progress = uiState.progress
    if (progress.totalCards == 0) {
        EmptyState(stringResource(R.string.statistics_empty))
        return
    }

    Text(
        text = stringResource(R.string.statistics_total, progress.totalCards),
        style = MaterialTheme.typography.titleLarge,
    )
    ProgressItem(R.string.statistics_new, progress.newCards, progress.totalCards)
    ProgressItem(R.string.statistics_learning, progress.learningCards, progress.totalCards)
    ProgressItem(R.string.statistics_mastered, progress.masteredCards, progress.totalCards)
    ProgressItem(R.string.statistics_problematic, progress.problematicCards, progress.totalCards)
    ProgressItem(R.string.statistics_due, progress.dueCards, progress.totalCards)

    uiState.activity?.let { activity ->
        Text(
            text = stringResource(R.string.statistics_activity_title),
            style = MaterialTheme.typography.titleLarge,
        )
        PeriodItem(R.string.statistics_last_7_days, activity.last7DaysReviews)
        PeriodItem(R.string.statistics_last_30_days, activity.last30DaysReviews)
        PeriodItem(R.string.statistics_all_time, activity.allTimeReviews)
    }
}

@Composable
private fun PeriodItem(labelResource: Int, count: Int) {
    Text(
        stringResource(
            R.string.statistics_period_count,
            stringResource(labelResource),
            pluralStringResource(R.plurals.statistics_reviews, count, count),
        ),
    )
}

@Composable
private fun ProgressItem(labelResource: Int, count: Int, total: Int) {
    val label = stringResource(labelResource)
    Card(modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.statistics_count, label, count),
                style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(
                progress = { (count.toFloat() / total).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
