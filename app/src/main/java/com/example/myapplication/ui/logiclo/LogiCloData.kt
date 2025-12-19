package com.example.myapplication.ui.logiclo

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AllOut
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

// =============================================================================
// 1. Data Models & Constants
// =============================================================================

enum class ItemType { OUTER, TOP, BOTTOM }
enum class AppMode { CASUAL, OFFICE }
enum class EnvMode { OUTDOOR, INDOOR }
enum class CleaningType { HOME, DRY }
enum class FitType { SLIM, REGULAR, LOOSE }
enum class SleeveLength { SHORT, LONG, NONE }
enum class Thickness { THIN, NORMAL, THICK }

data class UiClothingItem(
    val id: String,
    val name: String,
    val brand: String = "",
    val type: ItemType,
    val sleeveLength: SleeveLength = SleeveLength.SHORT,
    val thickness: Thickness = Thickness.NORMAL,
    val color: Color,
    val icon: ImageVector,
    val maxWears: Int,
    var currentWears: Int = 0,
    var isDirty: Boolean = false,
    val cleaningType: CleaningType = CleaningType.HOME,
    val fit: FitType = FitType.REGULAR,
)

// Dummy data generation
fun generateMockItems(): List<UiClothingItem> {
    return listOf(
        // Tops
        UiClothingItem(id = "t1", name = "白Tシャツ", brand = "UNIQLO", type = ItemType.TOP, sleeveLength = SleeveLength.SHORT, thickness = Thickness.NORMAL, color = Color.White, icon = Icons.Default.Checkroom, maxWears = 1, fit = FitType.REGULAR),
        UiClothingItem(id = "t2", name = "ネイビーポロ", brand = "Lacoste", type = ItemType.TOP, sleeveLength = SleeveLength.SHORT, thickness = Thickness.NORMAL, color = Color(0xFF1A237E), icon = Icons.Default.Checkroom, maxWears = 1, fit = FitType.SLIM),
        UiClothingItem(id = "t3", name = "グレーパーカー", brand = "GU", type = ItemType.TOP, sleeveLength = SleeveLength.LONG, thickness = Thickness.THICK, color = Color.Gray, icon = Icons.Default.Checkroom, maxWears = 3, fit = FitType.LOOSE),
        UiClothingItem(id = "t4", name = "白シャツ", brand = "Brooks Brothers", type = ItemType.TOP, sleeveLength = SleeveLength.LONG, thickness = Thickness.THIN, color = Color.White, icon = Icons.Default.Checkroom, maxWears = 1),

        // Bottoms
        UiClothingItem(id = "b1", name = "デニム", brand = "Levi's", type = ItemType.BOTTOM, sleeveLength = SleeveLength.LONG, thickness = Thickness.THICK, color = Color(0xFF3F51B5), icon = Icons.Default.AccessibilityNew, maxWears = 10, fit = FitType.REGULAR),
        UiClothingItem(id = "b2", name = "黒スラックス", brand = "Global Work", type = ItemType.BOTTOM, sleeveLength = SleeveLength.LONG, thickness = Thickness.NORMAL, color = Color(0xFF212121), icon = Icons.Default.AccessibilityNew, maxWears = 3, fit = FitType.SLIM),
        UiClothingItem(id = "b3", name = "チノパン", brand = "UNIQLO", type = ItemType.BOTTOM, sleeveLength = SleeveLength.LONG, thickness = Thickness.NORMAL, color = Color(0xFF8D6E63), icon = Icons.Default.AccessibilityNew, maxWears = 5),

        // Outers
        UiClothingItem(id = "o1", name = "ネイビージャケット", brand = "United Arrows", type = ItemType.OUTER, sleeveLength = SleeveLength.LONG, thickness = Thickness.NORMAL, color = Color(0xFF1A237E), icon = Icons.Default.AllOut, maxWears = 5, cleaningType = CleaningType.DRY, fit = FitType.REGULAR),
        UiClothingItem(id = "o2", name = "ダウンコート", brand = "The North Face", type = ItemType.OUTER, sleeveLength = SleeveLength.LONG, thickness = Thickness.THICK, color = Color(0xFF424242), icon = Icons.Default.AllOut, maxWears = 20, cleaningType = CleaningType.DRY),
        UiClothingItem(id = "o3", name = "カーディガン", brand = "MUJI", type = ItemType.OUTER, sleeveLength = SleeveLength.LONG, thickness = Thickness.THIN, color = Color.Gray, icon = Icons.Default.AllOut, maxWears = 5),
    )
}