package com.example.myapplication.domain.model

import java.time.Instant

enum class WearFeedbackRating(val backendValue: String) {
    TOO_COLD("too_cold"),
    JUST_RIGHT("just_right"),
    TOO_WARM("too_warm");

    companion object {
        fun fromBackend(value: String?): WearFeedbackRating? = entries.firstOrNull { it.backendValue == value }
    }
}

data class WearFeedbackEntry(
    val id: String,
    val wornAt: Instant?,
    val topItemId: String?,
    val bottomItemId: String?,
    val rating: WearFeedbackRating?,
    val notes: String?,
    val submittedAt: Instant?
) {
    val isPending: Boolean get() = rating == null
}
