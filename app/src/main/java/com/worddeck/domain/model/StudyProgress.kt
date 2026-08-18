package com.worddeck.domain.model

/** Counts used by the overall and per-deck progress screens. */
data class StudyProgress(
    val newCards: Int = 0,
    val learningCards: Int = 0,
    val masteredCards: Int = 0,
    val problematicCards: Int = 0,
    val dueCards: Int = 0,
) {
    val totalCards: Int
        get() = newCards + learningCards + masteredCards + problematicCards
}
