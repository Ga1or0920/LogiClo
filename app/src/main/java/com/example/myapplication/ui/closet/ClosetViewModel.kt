package com.example.myapplication.ui.closet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.ClosetRepository
import com.example.myapplication.domain.model.ClothingItem
import com.example.myapplication.domain.model.LaundryStatus
import com.example.myapplication.ui.closet.model.ClosetItemUi
import com.example.myapplication.ui.closet.model.ClosetFilters
import com.example.myapplication.ui.closet.model.ClosetUiState
import com.example.myapplication.ui.common.labelResId
import com.example.myapplication.util.time.InstantCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ClosetViewModel(
    private val closetRepository: ClosetRepository
) : ViewModel() {

    private val filterState = MutableStateFlow(LaundryStatus.CLOSET)
    private val attributeFilters = MutableStateFlow(ClosetFilters())
    private val filterDialogVisibility = MutableStateFlow(false)

    val uiState: StateFlow<ClosetUiState> = combine(
        closetRepository.observeAll(),
        filterState,
        attributeFilters,
        filterDialogVisibility
    ) { items, statusFilter, filters, isDialogVisible ->
        val counts = items.groupingBy { it.status }.eachCount()
        val statusFilteredItems = items.filter { item ->
            statusFilter == LaundryStatus.UNKNOWN || item.status == statusFilter
        }
        val filteredItems = statusFilteredItems.filter { item -> filters.matches(item) }
        val availableCategories = statusFilteredItems.map { it.category }.distinct()
        val availableTypes = statusFilteredItems.map { it.type }.distinct()
        val availableColorGroups = statusFilteredItems.map { it.colorGroup }.distinct()
        ClosetUiState(
            isLoading = false,
            filter = statusFilter,
            items = filteredItems.map { item ->
                ClosetItemUi(
                    id = item.id,
                    name = item.name,
                    brand = item.brand,
                    category = item.category,
                    categoryLabelResId = item.category.labelResId(),
                    colorHex = item.colorHex,
                    sleeveLength = item.sleeveLength,
                    status = item.status,
                    currentWears = item.currentWears,
                    maxWears = item.maxWears,
                    isAlwaysWash = item.isAlwaysWash,
                    cleaningType = item.cleaningType
                )
            },
            statusCounts = counts,
            filters = filters,
            availableCategories = availableCategories,
            availableTypes = availableTypes,
            availableColorGroups = availableColorGroups,
            isFilterDialogVisible = isDialogVisible
        )
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
        initialValue = ClosetUiState()
    )

    fun onFilterSelected(status: LaundryStatus) {
        filterState.value = status
    }

    fun onResetFilter() {
        filterState.value = LaundryStatus.CLOSET
    }

    fun onDeleteItem(id: String) {
        viewModelScope.launch {
            closetRepository.delete(id)
        }
    }

    fun onFilterButtonClicked() {
        filterDialogVisibility.value = true
    }

    fun onFilterDialogDismissed() {
        filterDialogVisibility.value = false
    }

    fun onFiltersApplied(filters: ClosetFilters) {
        attributeFilters.value = filters
        filterDialogVisibility.value = false
    }

    fun onFiltersCleared() {
        attributeFilters.value = ClosetFilters()
    }

    fun onIncrementWearCount(itemId: String) {
        viewModelScope.launch {
            val item = closetRepository.getItem(itemId) ?: return@launch
            val updatedWear = (item.currentWears + 1).coerceAtMost(item.maxWears)
            val shouldMarkDirty = item.isAlwaysWash || updatedWear >= item.maxWears
            val updatedStatus = if (shouldMarkDirty) LaundryStatus.DIRTY else LaundryStatus.CLOSET
            val lastWorn = InstantCompat.nowOrNull() ?: item.lastWornDate
            closetRepository.upsert(
                item.copy(
                    currentWears = updatedWear,
                    status = updatedStatus,
                    lastWornDate = lastWorn
                )
            )
        }
    }

    fun onMarkItemDirty(itemId: String) {
        viewModelScope.launch {
            val item = closetRepository.getItem(itemId) ?: return@launch
            val lastWorn = InstantCompat.nowOrNull() ?: item.lastWornDate
            closetRepository.upsert(
                item.copy(
                    status = LaundryStatus.DIRTY,
                    currentWears = item.maxWears,
                    lastWornDate = lastWorn
                )
            )
        }
    }

    fun onResetWearCount(itemId: String) {
        viewModelScope.launch {
            val item = closetRepository.getItem(itemId) ?: return@launch
            closetRepository.upsert(
                item.copy(
                    status = LaundryStatus.CLOSET,
                    currentWears = 0
                )
            )
        }
    }

    private fun ClosetFilters.matches(item: ClothingItem): Boolean {
        if (category != null && item.category != category) return false
        if (type != null && item.type != type) return false
        if (colorGroup != null && item.colorGroup != colorGroup) return false
        return true
    }

    class Factory(
        private val closetRepository: ClosetRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ClosetViewModel::class.java)) {
                return ClosetViewModel(closetRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
