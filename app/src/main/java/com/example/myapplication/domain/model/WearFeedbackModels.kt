package com.example.myapplication.domain.model

import java.util.Date

data class WearFeedback(
    val id: String,
    val wornAt: Date,
    val itemIds: List<String>,
    val isComfortable: Boolean? = null
)
