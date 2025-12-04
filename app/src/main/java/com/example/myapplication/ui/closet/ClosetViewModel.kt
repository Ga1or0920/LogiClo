package com.example.myapplication.ui.closet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.ClosetRepository
import com.example.myapplication.domain.model.LaundryStatus
import com.example.myapplication.ui.closet.model.ClosetItemUi
import com.example.myapplication.ui.closet.model.ClosetUiState
import com.example.myapplication.ui.common.labelResId
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

    val uiState: StateFlow<ClosetUiState> = combine(
        closetRepository.observeAll(),
        filterState
    ) { items, filter ->
        val counts = items.groupingBy { it.status }.eachCount()
        val filteredItems = items.filter { item ->
            if (filter == LaundryStatus.UNKNOWN) {
                true
            } else {
                item.status == filter
            }
        }
        ClosetUiState(
            isLoading = false,
            filter = filter,
            items = filteredItems.map { item ->
                ClosetItemUi(
                    id = item.id,
                    name = item.name,
                    brand = item.brand,
                    category = item.category,
                    categoryLabelResId = item.category.labelResId(),
                    colorHex = item.colorHex,
                    status = item.status,
                    currentWears = item.currentWears,
                    maxWears = item.maxWears,
                    isAlwaysWash = item.isAlwaysWash,
                    cleaningType = item.cleaningType
                )
            },
            statusCounts = counts
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
