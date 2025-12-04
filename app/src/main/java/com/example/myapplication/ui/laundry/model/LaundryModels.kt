package com.example.myapplication.ui.laundry.model

import androidx.annotation.StringRes
import com.example.myapplication.domain.model.CleaningType
import com.example.myapplication.domain.model.ClothingCategory
import com.example.myapplication.domain.model.LaundryStatus

data class LaundryItemUi(
    val id: String,
    val name: String,
    val category: ClothingCategory,
    @param:StringRes val categoryLabelResId: Int,
    val colorHex: String,
    val status: LaundryStatus,
    val cleaningType: CleaningType,
    val lastWornLabel: String? = null
)

enum class LaundryTab {
    HOME,
    DRY
}

data class LaundryUiState(
    val isLoading: Boolean = true,
    val isProcessing: Boolean = false,
    val activeTab: LaundryTab = LaundryTab.HOME,
    val homeLaundryItems: List<LaundryItemUi> = emptyList(),
    val dryCleaningItems: List<LaundryItemUi> = emptyList()
)
