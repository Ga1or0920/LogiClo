package com.example.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.myapplication.domain.model.CleaningType
import com.example.myapplication.domain.model.ClothingCategory
import com.example.myapplication.domain.model.ClothingItem
import com.example.myapplication.domain.model.ClothingType
import com.example.myapplication.domain.model.ColorGroup
import com.example.myapplication.domain.model.LaundryStatus
import com.example.myapplication.domain.model.Pattern
import com.example.myapplication.domain.model.SleeveLength
import com.example.myapplication.domain.model.Thickness
import java.util.Date

@Entity(tableName = "clothing_items")
data class ClothingItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val type: String,
    val sleeveLength: String,
    val thickness: String,
    val comfortMinCelsius: Double?,
    val comfortMaxCelsius: Double?,
    val colorHex: String,
    val colorGroup: String,
    val pattern: String,
    val maxWears: Int,
    val currentWears: Int,
    val isAlwaysWash: Boolean,
    val cleaningType: String,
    val status: String,
    val brand: String?,
    val imageUrl: String?,
    val lastWornDate: Long?
)

fun ClothingItemEntity.toDomain(): ClothingItem = ClothingItem(
    id = id,
    name = name,
    category = ClothingCategory.fromBackend(category),
    type = ClothingType.fromBackend(type),
    sleeveLength = SleeveLength.fromBackend(sleeveLength),
    thickness = Thickness.fromBackend(thickness),
    comfortMinCelsius = comfortMinCelsius,
    comfortMaxCelsius = comfortMaxCelsius,
    colorHex = colorHex,
    colorGroup = ColorGroup.fromBackend(colorGroup),
    pattern = Pattern.fromBackend(pattern),
    maxWears = maxWears,
    currentWears = currentWears,
    isAlwaysWash = isAlwaysWash,
    cleaningType = CleaningType.fromBackend(cleaningType),
    status = LaundryStatus.fromBackend(status),
    brand = brand,
    imageUrl = imageUrl,
    lastWornDate = lastWornDate?.let { Date(it) }
)

fun ClothingItem.toEntity(): ClothingItemEntity = ClothingItemEntity(
    id = id,
    name = name,
    category = category.backendValue,
    type = type.backendValue,
    sleeveLength = sleeveLength.backendValue,
    thickness = thickness.backendValue,
    comfortMinCelsius = comfortMinCelsius,
    comfortMaxCelsius = comfortMaxCelsius,
    colorHex = colorHex,
    colorGroup = colorGroup.backendValue,
    pattern = pattern.backendValue,
    maxWears = maxWears,
    currentWears = currentWears,
    isAlwaysWash = isAlwaysWash,
    cleaningType = cleaningType.backendValue,
    status = status.backendValue,
    brand = brand,
    imageUrl = imageUrl,
    lastWornDate = lastWornDate?.time
)
