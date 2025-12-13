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
import com.example.myapplication.domain.model.UserPreferences
import com.example.myapplication.domain.model.WeatherSnapshot
import java.util.Date

object SampleData {
    val closetItems = listOf(
        ClothingItem(
            id = "1",
            name = "White T-Shirt",
            category = ClothingCategory.T_SHIRT,
            type = ClothingType.TOP,
            sleeveLength = SleeveLength.SHORT,
            thickness = Thickness.NORMAL,
            colorHex = "#FFFFFF",
            colorGroup = ColorGroup.MONOTONE,
            pattern = Pattern.SOLID,
            maxWears = 2,
            currentWears = 0,
            isAlwaysWash = false,
            cleaningType = CleaningType.HOME,
            status = LaundryStatus.CLOSET,
            lastWornDate = null
        ),
    )

    val defaultUserPreferences = UserPreferences()

    val weather = WeatherSnapshot(
        minTemperatureCelsius = 15.0,
        maxTemperatureCelsius = 25.0,
        humidityPercent = 50,
        updatedAt = Date()
    )
}
