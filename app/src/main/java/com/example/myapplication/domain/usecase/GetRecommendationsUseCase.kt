package com.example.myapplication.domain.usecase

import com.example.myapplication.domain.model.ClothingItem
import com.example.myapplication.domain.model.UserPreferences
import com.example.myapplication.ui.common.UiMessage

class GetRecommendationsUseCase {
    operator fun invoke(
        closet: List<ClothingItem>,
        preferences: UserPreferences
    ): List<UiMessage> {
        // TODO: Implement recommendation logic
        return emptyList()
    }
}
