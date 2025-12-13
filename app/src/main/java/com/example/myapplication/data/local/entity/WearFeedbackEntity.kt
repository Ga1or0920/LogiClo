package com.example.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.myapplication.domain.model.WearFeedback
import java.util.Date

@Entity(tableName = "wear_feedback")
data class WearFeedbackEntity(
    @PrimaryKey val id: String,
    val wornAt: Long,
    val itemIds: String,
    val isComfortable: Boolean? = null
)

fun WearFeedbackEntity.toDomain(): WearFeedback = WearFeedback(
    id = id,
    wornAt = Date(wornAt),
    itemIds = itemIds.split(","),
    isComfortable = isComfortable
)

fun WearFeedback.toEntity(): WearFeedbackEntity = WearFeedbackEntity(
    id = id,
    wornAt = wornAt.time,
    itemIds = itemIds.joinToString(","),
    isComfortable = isComfortable
)
