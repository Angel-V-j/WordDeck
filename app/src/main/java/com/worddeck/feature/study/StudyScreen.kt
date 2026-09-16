package com.worddeck.feature.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.worddeck.R
import com.worddeck.common.OperationStatus
import com.worddeck.domain.model.ReviewRating
import com.worddeck.ui.components.BackButton
import com.worddeck.ui.components.EmptyState

@Composable
fun StudyScreen(
    uiState: StudyUiState,
    onRevealAnswer: () -> Unit,
    onTypedAnswerChange: (String) -> Unit,
    onSubmitTypedAnswer: () -> Unit,
    onRate: (ReviewRating) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(uiState.stage) {
        if (uiState.stage == StudyStage.ANSWER_REVEALED) {
            focusManager.clearFocus()
            keyboard?.hide()
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BackButton(onClick = onBack)
        Text(
            text = stringResource(R.string.study_title),
            style = MaterialTheme.typography.headlineMedium,
        )

        val currentCard = uiState.currentCard
        if (currentCard == null) {
            if (uiState.totalCards == 0) {
                EmptyState(stringResource(R.string.no_due_cards))
            } else {
                Text(
                    text = stringResource(R.string.study_complete),
                    style = MaterialTheme.typography.titleLarge,
                )
                SessionSummary(uiState)
            }
            return@Column
        }

        Text(
            stringResource(
                R.string.study_progress,
                uiState.position,
                uiState.totalCards,
            ),
        )

        LinearProgressIndicator(
            progress = { (uiState.position.toFloat() / uiState.totalCards).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
        Card(modifier = Modifier.fillMaxWidth().heightIn(min = 156.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = currentCard.flashcard.front.value,
                    style = MaterialTheme.typography.headlineSmall,
                )

                if (uiState.stage == StudyStage.ANSWER_REVEALED) {
                    HorizontalDivider()
                    Text(
                        text = stringResource(
                            if (uiState.mode == StudyMode.TYPED_ANSWER) {
                                R.string.expected_answer_label
                            } else {
                                R.string.answer_label
                            },
                        ),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = currentCard.flashcard.back.value,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    currentCard.flashcard.exampleSentence?.let { Text(it) }
                    currentCard.flashcard.additionalInformation?.let { Text(it) }
                }
            }
        }

        if (uiState.stage == StudyStage.QUESTION) {
            if (uiState.mode == StudyMode.FLASHCARD) {
                Button(
                    onClick = onRevealAnswer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.show_answer_action))
                }
            } else {
                OutlinedTextField(
                    shape = MaterialTheme.shapes.medium,
                    value = uiState.typedAnswer,
                    onValueChange = onTypedAnswerChange,
                    label = { Text(stringResource(R.string.typed_answer_label)) },
                    isError = uiState.typedAnswerError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (uiState.typedAnswerError) {
                    Text(
                        text = stringResource(R.string.typed_answer_required),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Button(
                    onClick = onSubmitTypedAnswer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.check_answer_action))
                }
            }
        } else if (uiState.stage == StudyStage.ANSWER_REVEALED) {
            if (uiState.mode == StudyMode.TYPED_ANSWER) {
                Text(
                    text = stringResource(
                        if (uiState.typedAnswerResult == TypedAnswerResult.CORRECT) {
                            R.string.typed_answer_correct
                        } else {
                            R.string.typed_answer_incorrect
                        },
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (uiState.reviewStatus == OperationStatus.ERROR) {
                Text(
                    text = stringResource(R.string.review_save_error),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (uiState.reviewStatus == OperationStatus.LOADING) {
                Text(stringResource(R.string.saving_review))
            }
            Text(stringResource(R.string.rate_answer_prompt))
            val ratingEnabled = uiState.reviewStatus != OperationStatus.LOADING
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RatingButton(R.string.rating_again, ReviewRating.AGAIN, ratingEnabled, onRate, Modifier.weight(1f))
                RatingButton(R.string.rating_hard, ReviewRating.HARD, ratingEnabled, onRate, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RatingButton(R.string.rating_good, ReviewRating.GOOD, ratingEnabled, onRate, Modifier.weight(1f))
                RatingButton(R.string.rating_easy, ReviewRating.EASY, ratingEnabled, onRate, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SessionSummary(uiState: StudyUiState) {
    Text(stringResource(R.string.session_reviews, uiState.ratings.size))
    Text(
        stringResource(
            R.string.session_rating_count,
            stringResource(R.string.rating_again),
            uiState.ratings.values.count { it == ReviewRating.AGAIN },
        ),
    )
    Text(
        stringResource(
            R.string.session_rating_count,
            stringResource(R.string.rating_hard),
            uiState.ratings.values.count { it == ReviewRating.HARD },
        ),
    )
    Text(
        stringResource(
            R.string.session_rating_count,
            stringResource(R.string.rating_good),
            uiState.ratings.values.count { it == ReviewRating.GOOD },
        ),
    )
    Text(
        stringResource(
            R.string.session_rating_count,
            stringResource(R.string.rating_easy),
            uiState.ratings.values.count { it == ReviewRating.EASY },
        ),
    )
    if (uiState.mode == StudyMode.TYPED_ANSWER) {
        Text(stringResource(R.string.session_correct, uiState.correctAnswers))
        Text(stringResource(R.string.session_incorrect, uiState.incorrectAnswers))
    }
}

@Composable
private fun RatingButton(
    label: Int,
    rating: ReviewRating,
    enabled: Boolean,
    onRate: (ReviewRating) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (rating) {
        ReviewRating.AGAIN -> colors.errorContainer to colors.onErrorContainer
        ReviewRating.HARD -> colors.tertiaryContainer to colors.onTertiaryContainer
        ReviewRating.GOOD -> colors.secondaryContainer to colors.onSecondaryContainer
        ReviewRating.EASY -> colors.primaryContainer to colors.onPrimaryContainer
    }
    Button(
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        onClick = { onRate(rating) },
        enabled = enabled,
        modifier = modifier.heightIn(min = 52.dp),
    ) {
        Text(stringResource(label))
    }
}
