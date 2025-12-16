package com.example.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wear_feedback_entries")
data class WearFeedbackEntity(
    @PrimaryKey val id: String,
    val wornAtEpochMillis: Long,
    val topItemId: String?,
    val bottomItemId: String?,
    val rating: String?,
    val topRating: String? = null,
    val bottomRating: String? = null,
    val notes: String?,
    val submittedAtEpochMillis: Long?
)
