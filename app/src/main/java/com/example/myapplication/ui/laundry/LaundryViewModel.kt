package com.example.myapplication.ui.laundry

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.R
import com.example.myapplication.data.repository.ClosetRepository
import com.example.myapplication.domain.model.CleaningType
import com.example.myapplication.domain.model.ClothingCategory
import com.example.myapplication.domain.model.ClothingItem
import com.example.myapplication.domain.model.LaundryStatus
import com.example.myapplication.domain.model.formatClothingDisplayLabel
import com.example.myapplication.ui.common.UiMessage
import com.example.myapplication.ui.common.UiMessageArg
import com.example.myapplication.ui.common.labelResId
import com.example.myapplication.ui.laundry.model.LaundryItemUi
import com.example.myapplication.ui.laundry.model.LaundryTab
import com.example.myapplication.ui.laundry.model.LaundryUiState
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LaundryViewModel(
    private val closetRepository: ClosetRepository,
    private val stringResolver: (Int) -> String,
    private val fallbackItemName: String
) : ViewModel() {

    private val tabState = MutableStateFlow(LaundryTab.HOME)
    private val processingState = MutableStateFlow(false)

    private val _events = MutableSharedFlow<LaundryEvent>()
    val events: SharedFlow<LaundryEvent> = _events

    val uiState: StateFlow<LaundryUiState> = combine(
        closetRepository.observeAll(),
        tabState,
        processingState
    ) { items, tab, isProcessing ->
        val homeLaundry = items.filter {
            it.status == LaundryStatus.DIRTY && it.cleaningType == CleaningType.HOME
        }
        val dryCleaning = items.filter {
            it.cleaningType == CleaningType.DRY && (it.status == LaundryStatus.DIRTY || it.status == LaundryStatus.CLEANING)
        }

            LaundryUiState(
            isLoading = false,
            isProcessing = isProcessing,
            activeTab = tab,
            homeLaundryItems = homeLaundry.map { it.toLaundryItemUi() },
            dryCleaningItems = dryCleaning.map { it.toLaundryItemUi() }
        )
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
        initialValue = LaundryUiState()
    )

    fun onTabSelected(tab: LaundryTab) {
        tabState.value = tab
    }

    fun onWashAll() {
        val currentItems = uiState.value.homeLaundryItems
        if (currentItems.isEmpty() || processingState.value) return

        performUpdate(
            successMessageProvider = {
                val total = currentItems.size
                val highlight = currentItems.firstOrNull()?.let {
                    val categoryLabel = stringResolver(it.categoryLabelResId)
                    formatClothingDisplayLabel(categoryLabel, it.name, it.colorHex)
                } ?: run {
                    val categoryLabel = stringResolver(ClothingCategory.UNKNOWN.labelResId())
                    formatClothingDisplayLabel(categoryLabel, fallbackItemName, "")
                }
                when {
                    total <= 0 -> UiMessage(R.string.laundry_message_wash_none)
                    total == 1 -> UiMessage(
                        resId = R.string.laundry_message_wash_single,
                        args = listOf(UiMessageArg.Raw(highlight))
                    )
                    else -> UiMessage(
                        resId = R.string.laundry_message_wash_multiple,
                        args = listOf(
                            UiMessageArg.Raw(highlight),
                            UiMessageArg.Raw(total - 1)
                        )
                    )
                }
            }
        ) {
            currentItems.map { it.id }
                .updateItems { item ->
                    item.copy(status = LaundryStatus.CLOSET, currentWears = 0)
                }
        }
    }

    fun onSendToDryCleaning(id: String) {
        if (processingState.value) return
        performUpdate(successMessageProvider = {
            val label = closetRepository.observeAll().first()
                .firstOrNull { it.id == id }
                ?.let { item ->
                    val categoryLabel = stringResolver(item.category.labelResId())
                    formatClothingDisplayLabel(categoryLabel, item.name, item.colorHex)
                }
                ?: run {
                    val categoryLabel = stringResolver(ClothingCategory.UNKNOWN.labelResId())
                    formatClothingDisplayLabel(categoryLabel, fallbackItemName, "")
                }
            UiMessage(
                resId = R.string.laundry_message_send_to_cleaning,
                args = listOf(UiMessageArg.Raw(label))
            )
        }) {
            listOf(id).updateItems { item ->
                item.copy(status = LaundryStatus.CLEANING)
            }
        }
    }

    fun onReceiveFromDryCleaning(id: String) {
        if (processingState.value) return
        performUpdate(successMessageProvider = {
            val label = closetRepository.observeAll().first()
                .firstOrNull { it.id == id }
                ?.let { item ->
                    val categoryLabel = stringResolver(item.category.labelResId())
                    formatClothingDisplayLabel(categoryLabel, item.name, item.colorHex)
                }
                ?: run {
                    val categoryLabel = stringResolver(ClothingCategory.UNKNOWN.labelResId())
                    formatClothingDisplayLabel(categoryLabel, fallbackItemName, "")
                }
            UiMessage(
                resId = R.string.laundry_message_receive,
                args = listOf(UiMessageArg.Raw(label))
            )
        }) {
            listOf(id).updateItems { item ->
                item.copy(status = LaundryStatus.CLOSET, currentWears = 0)
            }
        }
    }

    private fun performUpdate(
        successMessageProvider: suspend () -> UiMessage,
        block: suspend () -> Unit
    ) {
        viewModelScope.launch {
            processingState.value = true
            try {
                block()
                _events.emit(LaundryEvent.ShowMessage(successMessageProvider()))
            } catch (t: Throwable) {
                _events.emit(
                    LaundryEvent.ShowMessage(UiMessage(R.string.laundry_message_error))
                )
            } finally {
                processingState.value = false
            }
        }
    }

    private suspend fun List<String>.updateItems(transform: (ClothingItem) -> ClothingItem) {
        val currentSnapshot = closetRepository.observeAll().first()
        val currentItems = currentSnapshot.filter { it.id in this }
        if (currentItems.isEmpty()) return
        closetRepository.upsert(currentItems.map(transform))
    }

    private fun ClothingItem.toLaundryItemUi(): LaundryItemUi = LaundryItemUi(
        id = id,
        name = name,
        category = category,
        categoryLabelResId = category.labelResId(),
        colorHex = colorHex,
        status = status,
        cleaningType = cleaningType,
        lastWornLabel = formatLastWorn(lastWornDate)
    )

    private fun formatLastWorn(date: Date?): String? {
        if (date == null) return null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        val formatter = DateTimeFormatter.ofPattern("M/d H:mm").withZone(ZoneId.systemDefault())
        return formatter.format(date.toInstant())
    }

    class Factory(
        private val closetRepository: ClosetRepository,
        private val stringResolver: (Int) -> String,
        private val fallbackItemName: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LaundryViewModel::class.java)) {
                return LaundryViewModel(
                    closetRepository = closetRepository,
                    stringResolver = stringResolver,
                    fallbackItemName = fallbackItemName
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

sealed interface LaundryEvent {
    data class ShowMessage(val message: UiMessage) : LaundryEvent
}
