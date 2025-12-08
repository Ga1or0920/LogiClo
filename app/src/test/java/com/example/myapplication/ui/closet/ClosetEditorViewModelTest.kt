package com.example.myapplication.ui.closet

import com.example.myapplication.data.repository.ClosetRepository
import com.example.myapplication.data.repository.UserPreferencesRepository
import com.example.myapplication.domain.model.CleaningType
import com.example.myapplication.domain.model.ClothingCategory
import com.example.myapplication.domain.model.ClothingItem
import com.example.myapplication.domain.model.ClothingType
import com.example.myapplication.domain.model.ColorGroup
import com.example.myapplication.domain.model.LaundryStatus
import com.example.myapplication.domain.model.Pattern
import com.example.myapplication.domain.model.SleeveLength
import com.example.myapplication.domain.model.Thickness
import com.example.myapplication.domain.model.UserPreferences
import com.example.myapplication.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClosetEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadExistingItem_populatesUiStateWithPersistedValues() = runTest(mainDispatcherRule.dispatcher()) {
        val existingItem = ClothingItem(
            id = "existing-1",
            name = "ネイビーポロ",
            category = ClothingCategory.POLO,
            type = ClothingType.TOP,
            sleeveLength = SleeveLength.SHORT,
            thickness = Thickness.THIN,
            comfortMinCelsius = 19.0,
            comfortMaxCelsius = 29.0,
            colorHex = "#0D3B66",
            colorGroup = ColorGroup.NAVY_BLUE,
            pattern = Pattern.SOLID,
            maxWears = 4,
            currentWears = 2,
            isAlwaysWash = false,
            cleaningType = CleaningType.HOME,
            status = LaundryStatus.CLOSET,
            brand = "Sample Brand",
            imageUrl = null,
            lastWornDate = null
        )
        val closetRepository = FakeClosetRepository(listOf(existingItem))
        val preferencesRepository = FakeUserPreferencesRepository()

        val viewModel = ClosetEditorViewModel(
            closetRepository = closetRepository,
            userPreferencesRepository = preferencesRepository,
            existingItemId = existingItem.id
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isEditMode)
        assertEquals(existingItem.name, state.name)
        assertEquals(existingItem.brand ?: "", state.brand)
        assertNotNull(state.selectedCategory)
        assertEquals(existingItem.category, state.selectedCategory?.category)
        assertNotNull(state.selectedColor)
        assertEquals(existingItem.colorHex, state.selectedColor?.colorHex)
        assertEquals(existingItem.cleaningType, state.cleaningType)
        assertEquals(existingItem.maxWears, state.maxWears)
        assertFalse(state.isAlwaysWash)
        assertEquals(existingItem.comfortMinCelsius, state.comfortMinCelsius)
        assertEquals(existingItem.comfortMaxCelsius, state.comfortMaxCelsius)
        assertTrue(state.isComfortRangeCustomized)
    }

    @Test
    fun onSave_updatesExistingItemAndUserPreferences() = runTest(mainDispatcherRule.dispatcher()) {
        val existingItem = ClothingItem(
            id = "existing-2",
            name = "チャコールパンツ",
            category = ClothingCategory.SLACKS,
            type = ClothingType.BOTTOM,
            sleeveLength = SleeveLength.NONE,
            thickness = Thickness.NORMAL,
            comfortMinCelsius = 15.0,
            comfortMaxCelsius = 25.0,
            colorHex = "#808080",
            colorGroup = ColorGroup.MONOTONE,
            pattern = Pattern.SOLID,
            maxWears = 6,
            currentWears = 3,
            isAlwaysWash = false,
            cleaningType = CleaningType.DRY,
            status = LaundryStatus.CLOSET,
            brand = "Tailor",
            imageUrl = null,
            lastWornDate = null
        )
        val closetRepository = FakeClosetRepository(listOf(existingItem))
        val preferencesRepository = FakeUserPreferencesRepository()

        val viewModel = ClosetEditorViewModel(
            closetRepository = closetRepository,
            userPreferencesRepository = preferencesRepository,
            existingItemId = existingItem.id
        )

        advanceUntilIdle()

        viewModel.onAlwaysWashChanged(true)
        viewModel.onSave()

        advanceUntilIdle()

        val savedItem = closetRepository.lastUpserted
        assertNotNull(savedItem)
        assertTrue(savedItem!!.isAlwaysWash)
        assertEquals(1, savedItem.maxWears)
        val updatedPrefs = preferencesRepository.lastUpdatedPreferences
        assertNotNull(updatedPrefs)
        assertEquals(1, updatedPrefs!!.defaultMaxWears[existingItem.category])
        assertTrue(viewModel.uiState.value.saveCompleted)
    }

    private class FakeClosetRepository(initialItems: List<ClothingItem>) : ClosetRepository {
        private val items = initialItems.associateBy { it.id }.toMutableMap()
        private val state = MutableStateFlow(items.values.toList())
        var lastUpserted: ClothingItem? = null

        override fun observeAll(): Flow<List<ClothingItem>> = state

        override fun observeByStatus(status: LaundryStatus): Flow<List<ClothingItem>> {
            return state.map { list -> list.filter { it.status == status } }
        }

        override suspend fun upsert(item: ClothingItem) {
            items[item.id] = item
            lastUpserted = item
            state.value = items.values.toList()
        }

        override suspend fun upsert(items: List<ClothingItem>) {
            items.forEach { item ->
                this.items[item.id] = item
                lastUpserted = item
            }
            state.value = this.items.values.toList()
        }

        override suspend fun delete(id: String) {
            items.remove(id)
            state.value = items.values.toList()
        }

        override suspend fun getItem(id: String): ClothingItem? = items[id]

        override suspend fun getItems(ids: Collection<String>): List<ClothingItem> {
            return ids.mapNotNull { id -> items[id] }
        }
    }

    private class FakeUserPreferencesRepository(
        initial: UserPreferences = UserPreferences()
    ) : UserPreferencesRepository {
        private val state = MutableStateFlow(initial)
        var lastUpdatedPreferences: UserPreferences? = null

        override fun observe(): Flow<UserPreferences> = state

        override suspend fun upsert(preferences: UserPreferences) {
            state.value = preferences
            lastUpdatedPreferences = preferences
        }

        override suspend fun update(transform: (UserPreferences) -> UserPreferences) {
            val updated = transform(state.value)
            state.value = updated
            lastUpdatedPreferences = updated
        }

        override suspend fun updateLastSelectedMode(mode: com.example.myapplication.domain.model.TpoMode) {
            val updated = state.value.copy(lastSelectedMode = mode)
            state.value = updated
            lastUpdatedPreferences = updated
        }

        override suspend fun updateLastSelectedEnvironment(mode: com.example.myapplication.domain.model.EnvironmentMode) {
            val updated = state.value.copy(lastSelectedEnvironment = mode)
            state.value = updated
            lastUpdatedPreferences = updated
        }
    }
}
