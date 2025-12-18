package com.example.myapplication.ui.port

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.UUID

enum class ItemType { Outer, Top, Bottom }
enum class AppMode { Casual, Office }
enum class EnvMode { Outdoor, Drive, Indoor }
enum class CleaningType { Home, Dry }
enum class FitType { Slim, Regular, Loose }
enum class SleeveLength { Short, Long, None }
enum class Thickness { Thin, Normal, Thick }

data class ClothingItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val brand: String = "",
    val type: ItemType,
    val sleeveLength: SleeveLength = SleeveLength.Short,
    val thickness: Thickness = Thickness.Normal,
    val color: Color,
    val icon: ImageVector,
    val maxWears: Int,
    var currentWears: Int = 0,
    var isDirty: Boolean = false,
    val cleaningType: CleaningType = CleaningType.Home,
    val fit: FitType = FitType.Regular
)

object AppColors {
    val BgGrey = Color(0xFFF2F4F7)
    val CardWhite = Color.White
    val TextBlack = Color(0xFF2D3436)
    val TextGrey = Color(0xFF636E72)
    val AccentBlue = Color(0xFF0984E3)
    val AccentRed = Color(0xFFD63031)
    val Navy = Color(0xFF2C3E50)
}

fun generateMockItems(): MutableList<ClothingItem> {
    return mutableListOf(
        // Tops
        ClothingItem(id = "t1", name = "白Tシャツ", brand = "UNIQLO", type = ItemType.Top, sleeveLength = SleeveLength.Short, thickness = Thickness.Normal, color = Color.White, icon = Icons.Default.Checkroom, maxWears = 1, fit = FitType.Regular),
        ClothingItem(id = "t2", name = "ネイビーポロ", brand = "Lacoste", type = ItemType.Top, sleeveLength = SleeveLength.Short, thickness = Thickness.Normal, color = Color(0xFF1A237E), icon = Icons.Default.Checkroom, maxWears = 1, fit = FitType.Slim),
        ClothingItem(id = "t3", name = "グレーパーカー", brand = "GU", type = ItemType.Top, sleeveLength = SleeveLength.Long, thickness = Thickness.Thick, color = Color.Gray, icon = Icons.Default.Checkroom, maxWears = 3, fit = FitType.Loose),
        ClothingItem(id = "t4", name = "白シャツ", brand = "Brooks Brothers", type = ItemType.Top, sleeveLength = SleeveLength.Long, thickness = Thickness.Thin, color = Color.White, icon = Icons.Default.Checkroom, maxWears = 1),

        // Bottoms
        ClothingItem(id = "b1", name = "デニム", brand = "Levi's", type = ItemType.Bottom, sleeveLength = SleeveLength.Long, thickness = Thickness.Thick, color = Color(0xFF3F51B5), icon = Icons.Default.AccessibilityNew, maxWears = 10, fit = FitType.Regular),
        ClothingItem(id = "b2", name = "黒スラックス", brand = "Global Work", type = ItemType.Bottom, sleeveLength = SleeveLength.Long, thickness = Thickness.Normal, color = Color(0xFF212121), icon = Icons.Default.AccessibilityNew, maxWears = 3, fit = FitType.Slim),
        ClothingItem(id = "b3", name = "チノパン", brand = "UNIQLO", type = ItemType.Bottom, sleeveLength = SleeveLength.Long, thickness = Thickness.Normal, color = Color(0xFF8D6E63), icon = Icons.Default.AccessibilityNew, maxWears = 5),

        // Outers
        ClothingItem(id = "o1", name = "ネイビージャケット", brand = "United Arrows", type = ItemType.Outer, sleeveLength = SleeveLength.Long, thickness = Thickness.Normal, color = Color(0xFF1A237E), icon = Icons.Default.AllOut, maxWears = 5, cleaningType = CleaningType.Dry, fit = FitType.Regular),
        ClothingItem(id = "o2", name = "ダウンコート", brand = "The North Face", type = ItemType.Outer, sleeveLength = SleeveLength.Long, thickness = Thickness.Thick, color = Color(0xFF424242), icon = Icons.Default.AllOut, maxWears = 20, cleaningType = CleaningType.Dry),
        ClothingItem(id = "o3", name = "カーディガン", brand = "MUJI", type = ItemType.Outer, sleeveLength = SleeveLength.Long, thickness = Thickness.Thin, color = Color.Gray, icon = Icons.Default.AllOut, maxWears = 5)
    )
}
