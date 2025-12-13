package com.example.myapplication.domain.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * クローゼット関連のドメインモデルと列挙型をまとめて定義する。
 */

enum class ClothingCategory(val backendValue: String) {
    T_SHIRT("t_shirt"),
    POLO("polo"),
    DRESS_SHIRT("dress_shirt"),
    KNIT("knit"),
    SWEATSHIRT("sweatshirt"),
    DENIM("denim"),
    SLACKS("slacks"),
    CHINO("chino"),
    OUTER_LIGHT("outer_light"),
    DOWN("down"),
    COAT("coat"),
    INNER("inner"),
    JACKET("jacket"),
    UNKNOWN("unknown");

    companion object {
        fun fromBackend(value: String): ClothingCategory = entries.firstOrNull { it.backendValue == value }
            ?: UNKNOWN
    }
}

enum class ClothingType(val backendValue: String) {
    TOP("top"),
    BOTTOM("bottom"),
    OUTER("outer"),
    INNER("inner"),
    UNKNOWN("unknown");

    companion object {
        fun fromBackend(value: String): ClothingType = entries.firstOrNull { it.backendValue == value }
            ?: UNKNOWN
    }
}

enum class SleeveLength(val backendValue: String) {
    SHORT("short"),
    LONG("long"),
    NONE("none"),
    UNKNOWN("unknown");

    companion object {
        fun fromBackend(value: String): SleeveLength = entries.firstOrNull { it.backendValue == value }
            ?: UNKNOWN
    }
}

enum class Thickness(val backendValue: String) {
    THIN("thin"),
    NORMAL("normal"),
    THICK("thick"),
    UNKNOWN("unknown");

    companion object {
        fun fromBackend(value: String): Thickness = entries.firstOrNull { it.backendValue == value }
            ?: UNKNOWN
    }
}

enum class Pattern(val backendValue: String) {
    SOLID("solid"),
    STRIPE("stripe"),
    GRAPHIC("graphic"),
    UNKNOWN("unknown");

    companion object {
        fun fromBackend(value: String): Pattern = entries.firstOrNull { it.backendValue == value }
            ?: UNKNOWN
    }
}

enum class ColorGroup(val backendValue: String) {
    MONOTONE("monotone"),
    NAVY_BLUE("navy_blue"),
    VIVID("vivid"),
    EARTH_TONE("earth_tone"),
    PASTEL("pastel"),
    OTHER("other"),
    UNKNOWN("unknown");

    companion object {
        fun fromBackend(value: String): ColorGroup = entries.firstOrNull { it.backendValue == value }
            ?: UNKNOWN
    }
}

enum class LaundryStatus(val backendValue: String) {
    CLOSET("closet"),
    DIRTY("dirty"),
    CLEANING("cleaning"),
    UNKNOWN("unknown");

    companion object {
        fun fromBackend(value: String): LaundryStatus = entries.firstOrNull { it.backendValue == value }
            ?: UNKNOWN
    }
}

enum class CleaningType(val backendValue: String) {
    HOME("home"),
    DRY("dry"),
    UNKNOWN("unknown");

    companion object {
        fun fromBackend(value: String): CleaningType = entries.firstOrNull { it.backendValue == value }
            ?: UNKNOWN
    }
}

data class ClothingItem(
    val id: String = "",
    val name: String = "",
    val category: ClothingCategory = ClothingCategory.UNKNOWN,
    val type: ClothingType = ClothingType.UNKNOWN,
    val sleeveLength: SleeveLength = SleeveLength.UNKNOWN,
    val thickness: Thickness = Thickness.UNKNOWN,
    val comfortMinCelsius: Double? = null,
    val comfortMaxCelsius: Double? = null,
    val colorHex: String = "",
    val colorGroup: ColorGroup = ColorGroup.UNKNOWN,
    val pattern: Pattern = Pattern.UNKNOWN,
    val maxWears: Int = 1,
    val currentWears: Int = 0,
    val isAlwaysWash: Boolean = false,
    val cleaningType: CleaningType = CleaningType.UNKNOWN,
    val status: LaundryStatus = LaundryStatus.CLOSET,
    val brand: String? = null,
    val imageUrl: String? = null,
    val lastWornDate: Date? = null,
    @ServerTimestamp val createdAt: Date? = null,
    @ServerTimestamp val updatedAt: Date? = null
)
