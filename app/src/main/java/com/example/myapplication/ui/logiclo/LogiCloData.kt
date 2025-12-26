package com.example.myapplication.ui.logiclo

import androidx.compose.ui.graphics.Color
import com.example.myapplication.R

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
    val categoryKey: String,
    val sleeveLength: SleeveLength = SleeveLength.SHORT,
    val thickness: Thickness = Thickness.NORMAL,
    val color: Color,
    val icon: Int,
    val maxWears: Int,
    var currentWears: Int = 0,
    var isDirty: Boolean = false,
    val cleaningType: CleaningType = CleaningType.HOME,
    val fit: FitType = FitType.REGULAR,
    val comfortMinCelsius: Double? = null,
    val comfortMaxCelsius: Double? = null,
) {
    /** 袖丈に応じた表示用アイコンを返す */
    val displayIcon: Int
        get() = when (type) {
            ItemType.TOP -> when (sleeveLength) {
                SleeveLength.LONG -> R.drawable.ic_clothing_top_long
                else -> R.drawable.ic_clothing_top
            }
            ItemType.OUTER -> R.drawable.ic_clothing_outer
            ItemType.BOTTOM -> R.drawable.ic_clothing_bottom
        }
}

// Dummy data generation
fun generateMockItems(): List<UiClothingItem> {
    return listOf(
        // Tops
        UiClothingItem(id = "t1", name = "白Tシャツ", brand = "UNIQLO", type = ItemType.TOP, categoryKey = "t_shirt", sleeveLength = SleeveLength.SHORT, thickness = Thickness.NORMAL, color = Color.White, icon = R.drawable.ic_clothing_top, maxWears = 1, fit = FitType.REGULAR),
        UiClothingItem(id = "t2", name = "ネイビーポロ", brand = "Lacoste", type = ItemType.TOP, categoryKey = "polo", sleeveLength = SleeveLength.SHORT, thickness = Thickness.NORMAL, color = Color(0xFF1A237E), icon = R.drawable.ic_clothing_top, maxWears = 1, fit = FitType.SLIM),
        UiClothingItem(id = "t3", name = "グレーパーカー", brand = "GU", type = ItemType.TOP, categoryKey = "hoodie", sleeveLength = SleeveLength.LONG, thickness = Thickness.THICK, color = Color.Gray, icon = R.drawable.ic_clothing_top, maxWears = 3, fit = FitType.LOOSE),
        UiClothingItem(id = "t4", name = "白シャツ", brand = "Brooks Brothers", type = ItemType.TOP, categoryKey = "shirt", sleeveLength = SleeveLength.LONG, thickness = Thickness.THIN, color = Color.White, icon = R.drawable.ic_clothing_top, maxWears = 1),

        // Bottoms
        UiClothingItem(id = "b1", name = "デニム", brand = "Levi's", type = ItemType.BOTTOM, categoryKey = "denim", sleeveLength = SleeveLength.LONG, thickness = Thickness.THICK, color = Color(0xFF3F51B5), icon = R.drawable.ic_clothing_bottom, maxWears = 10, fit = FitType.REGULAR),
        UiClothingItem(id = "b2", name = "黒スラックス", brand = "Global Work", type = ItemType.BOTTOM, categoryKey = "slacks", sleeveLength = SleeveLength.LONG, thickness = Thickness.NORMAL, color = Color(0xFF212121), icon = R.drawable.ic_clothing_bottom, maxWears = 3, fit = FitType.SLIM),
        UiClothingItem(id = "b3", name = "チノパン", brand = "UNIQLO", type = ItemType.BOTTOM, categoryKey = "chino", sleeveLength = SleeveLength.LONG, thickness = Thickness.NORMAL, color = Color(0xFF8D6E63), icon = R.drawable.ic_clothing_bottom, maxWears = 5),

        // Outers
        UiClothingItem(id = "o1", name = "ネイビージャケット", brand = "United Arrows", type = ItemType.OUTER, categoryKey = "jacket", sleeveLength = SleeveLength.LONG, thickness = Thickness.NORMAL, color = Color(0xFF1A237E), icon = R.drawable.ic_clothing_outer, maxWears = 5, cleaningType = CleaningType.DRY, fit = FitType.REGULAR),
        UiClothingItem(id = "o2", name = "ダウンコート", brand = "The North Face", type = ItemType.OUTER, categoryKey = "coat", sleeveLength = SleeveLength.LONG, thickness = Thickness.THICK, color = Color(0xFF424242), icon = R.drawable.ic_clothing_outer, maxWears = 20, cleaningType = CleaningType.DRY),
        UiClothingItem(id = "o3", name = "カーディガン", brand = "MUJI", type = ItemType.OUTER, categoryKey = "knit", sleeveLength = SleeveLength.LONG, thickness = Thickness.THIN, color = Color.Gray, icon = R.drawable.ic_clothing_outer, maxWears = 5),
    )
}
