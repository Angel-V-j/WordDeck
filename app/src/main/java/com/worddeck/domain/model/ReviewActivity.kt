package com.worddeck.domain.model

/** Number of completed reviews in the required reporting periods. */
data class ReviewActivity(
    val last7DaysReviews: Int = 0,
    val last30DaysReviews: Int = 0,
    val allTimeReviews: Int = 0,
)
