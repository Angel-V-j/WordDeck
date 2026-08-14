package com.worddeck.feature.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.worddeck.R
import com.worddeck.domain.model.ReviewRating

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
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.back_action))
        }
        Text(
            text = stringResource(R.string.study_title),
            style = MaterialTheme.typography.headlineMedium,
        )

        val currentCard = uiState.currentCard
        if (currentCard == null) {
            Text(
                stringResource(
                    if (uiState.totalCards == 0) {
                        R.string.no_due_cards
                    } else {
                        R.string.study_complete
                    },
                ),
            )
            return@Column
        }

        Text(
            stringResource(
                R.string.study_progress,
                uiState.position,
                uiState.totalCards,
            ),
        )

        Card(modifier = Modifier.fillMaxWidth()) {
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
            Text(stringResource(R.string.rate_answer_prompt))
            RatingButton(R.string.rating_again, ReviewRating.AGAIN, onRate)
            RatingButton(R.string.rating_hard, ReviewRating.HARD, onRate)
            RatingButton(R.string.rating_good, ReviewRating.GOOD, onRate)
            RatingButton(R.string.rating_easy, ReviewRating.EASY, onRate)
        }
    }
}

@Composable
private fun RatingButton(
    label: Int,
    rating: ReviewRating,
    onRate: (ReviewRating) -> Unit,
) {
    OutlinedButton(
        onClick = { onRate(rating) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(label))
    }
}
