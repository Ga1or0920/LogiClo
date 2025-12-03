package com.example.myapplication.data.sample

import com.example.myapplication.domain.model.CleaningType
import com.example.myapplication.domain.model.ClothingCategory
import com.example.myapplication.domain.model.ClothingItem
import com.example.myapplication.domain.model.ClothingType
import com.example.myapplication.domain.model.ColorGroup
import com.example.myapplication.domain.model.LaundryStatus
import com.example.myapplication.domain.model.Pattern
import com.example.myapplication.domain.model.SleeveLength
import com.example.myapplication.domain.model.Thickness
import com.example.myapplication.domain.model.TpoMode
import com.example.myapplication.domain.model.UserPreferences
import com.example.myapplication.domain.model.EnvironmentMode
import com.example.myapplication.domain.model.WeatherSnapshot

object SampleData {

    val defaultUserPreferences: UserPreferences = UserPreferences(
        lastLogin = null,
        lastSelectedMode = TpoMode.OFFICE,
        lastSelectedEnvironment = EnvironmentMode.OUTDOOR,
        tempOffsets = mapOf(
            Thickness.THIN to -2,
            Thickness.THICK to 2
        ),
        colorRules = com.example.myapplication.domain.model.ColorRules(
            allowBlackNavy = true,
            disallowVividPair = true
        ),
        defaultMaxWears = mapOf(
            ClothingCategory.DENIM to 10,
            ClothingCategory.KNIT to 5
        )
    )

    val closetItems: List<ClothingItem> = listOf(
        ClothingItem(
            id = "item-001",
            name = "ライトブルーTシャツ",
            category = ClothingCategory.T_SHIRT,
            type = ClothingType.TOP,
            sleeveLength = SleeveLength.SHORT,
            thickness = Thickness.THIN,
            colorHex = "#ADD8E6",
            colorGroup = ColorGroup.PASTEL,
            pattern = Pattern.SOLID,
            maxWears = 2,
            currentWears = 0,
            isAlwaysWash = false,
            cleaningType = CleaningType.HOME,
            status = LaundryStatus.CLOSET,
            imageUrl = null,
            lastWornDate = null
        ),
        ClothingItem(
            id = "item-002",
            name = "グレーオックスフォードシャツ",
            category = ClothingCategory.DRESS_SHIRT,
            type = ClothingType.TOP,
            sleeveLength = SleeveLength.LONG,
            thickness = Thickness.THIN,
            colorHex = "#C0C0C0",
            colorGroup = ColorGroup.MONOTONE,
            pattern = Pattern.SOLID,
            maxWears = 3,
            currentWears = 1,
            isAlwaysWash = true,
            cleaningType = CleaningType.HOME,
            status = LaundryStatus.CLOSET,
            imageUrl = null,
            lastWornDate = null
        ),
        ClothingItem(
            id = "item-003",
            name = "ネイビーカーディガン",
            category = ClothingCategory.KNIT,
            type = ClothingType.TOP,
            sleeveLength = SleeveLength.LONG,
            thickness = Thickness.NORMAL,
            colorHex = "#1B2A52",
            colorGroup = ColorGroup.NAVY_BLUE,
            pattern = Pattern.SOLID,
            maxWears = 5,
            currentWears = 2,
            isAlwaysWash = false,
            cleaningType = CleaningType.HOME,
            status = LaundryStatus.CLOSET,
            imageUrl = null,
            lastWornDate = null
        ),
        ClothingItem(
            id = "item-004",
            name = "ベージュチノ",
            category = ClothingCategory.CHINO,
            type = ClothingType.BOTTOM,
            sleeveLength = SleeveLength.NONE,
            thickness = Thickness.NORMAL,
            colorHex = "#D2B48C",
            colorGroup = ColorGroup.EARTH_TONE,
            pattern = Pattern.SOLID,
            maxWears = 5,
            currentWears = 1,
            isAlwaysWash = false,
            cleaningType = CleaningType.HOME,
            status = LaundryStatus.CLOSET,
            imageUrl = null,
            lastWornDate = null
        ),
        ClothingItem(
            id = "item-005",
            name = "チャコールスラックス",
            category = ClothingCategory.SLACKS,
            type = ClothingType.BOTTOM,
            sleeveLength = SleeveLength.NONE,
            thickness = Thickness.NORMAL,
            colorHex = "#2F4F4F",
            colorGroup = ColorGroup.MONOTONE,
            pattern = Pattern.SOLID,
            maxWears = 6,
            currentWears = 0,
            isAlwaysWash = false,
            cleaningType = CleaningType.DRY,
            status = LaundryStatus.CLOSET,
            imageUrl = null,
            lastWornDate = null
        ),
        ClothingItem(
            id = "item-006",
            name = "ブラックデニム",
            category = ClothingCategory.DENIM,
            type = ClothingType.BOTTOM,
            sleeveLength = SleeveLength.NONE,
            thickness = Thickness.NORMAL,
            colorHex = "#111111",
            colorGroup = ColorGroup.MONOTONE,
            pattern = Pattern.SOLID,
            maxWears = 8,
            currentWears = 3,
            isAlwaysWash = false,
            cleaningType = CleaningType.HOME,
            status = LaundryStatus.CLOSET,
            imageUrl = null,
            lastWornDate = null
        ),
        ClothingItem(
            id = "item-007",
            name = "ライトグレーのジャケット",
            category = ClothingCategory.JACKET,
            type = ClothingType.OUTER,
            sleeveLength = SleeveLength.LONG,
            thickness = Thickness.NORMAL,
            colorHex = "#D3D3D3",
            colorGroup = ColorGroup.MONOTONE,
            pattern = Pattern.SOLID,
            maxWears = 4,
            currentWears = 1,
            isAlwaysWash = false,
            cleaningType = CleaningType.DRY,
            status = LaundryStatus.CLEANING,
            imageUrl = null,
            lastWornDate = null
        )
    )

    val weather: WeatherSnapshot = WeatherSnapshot(
        minTemperatureCelsius = 18.0,
        maxTemperatureCelsius = 27.0,
        humidityPercent = 65,
        updatedAt = null
    )
}
