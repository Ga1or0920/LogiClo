package com.example.myapplication.ui.closet

import com.example.myapplication.data.repository.InMemoryClosetRepository
import com.example.myapplication.domain.model.CleaningType
import com.example.myapplication.domain.model.ClothingCategory
import com.example.myapplication.domain.model.ClothingItem
import com.example.myapplication.domain.model.ClothingType
import com.example.myapplication.domain.model.ColorGroup
import com.example.myapplication.domain.model.LaundryStatus
import com.example.myapplication.domain.model.Pattern
import com.example.myapplication.domain.model.SleeveLength
import com.example.myapplication.domain.model.Thickness
import com.example.myapplication.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClosetViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun incrementWearCount_advancesWearAndKeepsClosetStatus() = runTest(mainDispatcherRule.dispatcher()) {
        val item = sampleItem(
            currentWears = 0,
            maxWears = 3,
            status = LaundryStatus.CLOSET
        )
        val repository = InMemoryClosetRepository(listOf(item))
        val viewModel = ClosetViewModel(repository)

        viewModel.onIncrementWearCount(item.id)
        advanceUntilIdle()

        val updated = repository.getItem(item.id)!!
        assertEquals(1, updated.currentWears)
        assertEquals(LaundryStatus.CLOSET, updated.status)
    }

    @Test
    fun incrementWearCount_marksDirtyWhenLimitReached() = runTest(mainDispatcherRule.dispatcher()) {
        val item = sampleItem(
            currentWears = 2,
            maxWears = 3,
            status = LaundryStatus.CLOSET
        )
        val repository = InMemoryClosetRepository(listOf(item))
        val viewModel = ClosetViewModel(repository)

        viewModel.onIncrementWearCount(item.id)
        advanceUntilIdle()

        val updated = repository.getItem(item.id)!!
        assertEquals(3, updated.currentWears)
        assertEquals(LaundryStatus.DIRTY, updated.status)
    }

    @Test
    fun markItemDirty_setsStatusAndMaxWear() = runTest(mainDispatcherRule.dispatcher()) {
        val item = sampleItem(
            currentWears = 1,
            maxWears = 3,
            status = LaundryStatus.CLOSET
        )
        val repository = InMemoryClosetRepository(listOf(item))
        val viewModel = ClosetViewModel(repository)

        viewModel.onMarkItemDirty(item.id)
        advanceUntilIdle()

        val updated = repository.getItem(item.id)!!
        assertEquals(3, updated.currentWears)
        assertEquals(LaundryStatus.DIRTY, updated.status)
    }

    @Test
    fun resetWearCount_restoresClosetStatus() = runTest(mainDispatcherRule.dispatcher()) {
        val item = sampleItem(
            currentWears = 3,
            maxWears = 3,
            status = LaundryStatus.DIRTY
        )
        val repository = InMemoryClosetRepository(listOf(item))
        val viewModel = ClosetViewModel(repository)

        viewModel.onResetWearCount(item.id)
        advanceUntilIdle()

        val updated = repository.getItem(item.id)!!
        assertEquals(0, updated.currentWears)
        assertEquals(LaundryStatus.CLOSET, updated.status)
    }

    private fun sampleItem(
        currentWears: Int,
        maxWears: Int,
        status: LaundryStatus
    ): ClothingItem = ClothingItem(
        id = "item-1",
        name = "ネイビージャケット",
        category = ClothingCategory.JACKET,
        type = ClothingType.OUTER,
        sleeveLength = SleeveLength.LONG,
        thickness = Thickness.THICK,
        comfortMinCelsius = null,
        comfortMaxCelsius = null,
        colorHex = "#0D3B66",
        colorGroup = ColorGroup.NAVY_BLUE,
        pattern = Pattern.SOLID,
        maxWears = maxWears,
        currentWears = currentWears,
        isAlwaysWash = false,
        cleaningType = CleaningType.HOME,
        status = status,
        brand = null,
        imageUrl = null,
        lastWornDate = null
    )
}
