package com.example.myapplication.domain.model

import java.time.Instant

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
    val id: String,
    val name: String,
    val category: ClothingCategory,
    val type: ClothingType,
    val sleeveLength: SleeveLength,
    val thickness: Thickness,
    val colorHex: String,
    val colorGroup: ColorGroup,
    val pattern: Pattern,
    val maxWears: Int,
    val currentWears: Int,
    val isAlwaysWash: Boolean,
    val cleaningType: CleaningType,
    val status: LaundryStatus,
    val brand: String? = null,
    val imageUrl: String? = null,
    val lastWornDate: Instant? = null
)
