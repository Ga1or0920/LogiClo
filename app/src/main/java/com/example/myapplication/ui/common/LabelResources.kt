package com.example.myapplication.ui.common

import androidx.annotation.StringRes
import com.example.myapplication.R
import com.example.myapplication.domain.model.CleaningType
import com.example.myapplication.domain.model.ClothingCategory
import com.example.myapplication.domain.model.ClothingType
import com.example.myapplication.domain.model.ColorGroup
import com.example.myapplication.domain.model.LaundryStatus
import com.example.myapplication.domain.model.SleeveLength
import com.example.myapplication.domain.model.Thickness

@StringRes
fun ClothingCategory.labelResId(): Int = when (this) {
    ClothingCategory.T_SHIRT -> R.string.clothing_category_t_shirt
    ClothingCategory.POLO -> R.string.clothing_category_polo
    ClothingCategory.DRESS_SHIRT -> R.string.clothing_category_dress_shirt
    ClothingCategory.KNIT -> R.string.clothing_category_knit
    ClothingCategory.SWEATSHIRT -> R.string.clothing_category_sweatshirt
    ClothingCategory.DENIM -> R.string.clothing_category_denim
    ClothingCategory.SLACKS -> R.string.clothing_category_slacks
    ClothingCategory.CHINO -> R.string.clothing_category_chino
    ClothingCategory.OUTER_LIGHT -> R.string.clothing_category_outer_light
    ClothingCategory.DOWN -> R.string.clothing_category_down
    ClothingCategory.COAT -> R.string.clothing_category_coat
    ClothingCategory.INNER -> R.string.clothing_category_inner
    ClothingCategory.JACKET -> R.string.clothing_category_jacket
    ClothingCategory.UNKNOWN -> R.string.clothing_category_unknown
}

@StringRes
fun CleaningType.labelResId(): Int = when (this) {
    CleaningType.HOME -> R.string.cleaning_type_home
    CleaningType.DRY -> R.string.cleaning_type_dry
    CleaningType.UNKNOWN -> R.string.cleaning_type_unknown
}

@StringRes
fun LaundryStatus.labelResId(): Int = when (this) {
    LaundryStatus.CLOSET -> R.string.laundry_status_closet
    LaundryStatus.DIRTY -> R.string.laundry_status_dirty
    LaundryStatus.CLEANING -> R.string.laundry_status_cleaning
    LaundryStatus.UNKNOWN -> R.string.laundry_status_unknown
}

@StringRes
fun ClothingType.labelResId(): Int = when (this) {
    ClothingType.TOP -> R.string.clothing_type_top
    ClothingType.BOTTOM -> R.string.clothing_type_bottom
    ClothingType.OUTER -> R.string.clothing_type_outer
    ClothingType.INNER -> R.string.clothing_type_inner
    ClothingType.UNKNOWN -> R.string.clothing_type_unknown
}

@StringRes
fun SleeveLength.labelResId(): Int = when (this) {
    SleeveLength.SHORT -> R.string.sleeve_length_short
    SleeveLength.LONG -> R.string.sleeve_length_long
    SleeveLength.NONE -> R.string.sleeve_length_none
    SleeveLength.UNKNOWN -> R.string.sleeve_length_unknown
}

@StringRes
fun Thickness.labelResId(): Int = when (this) {
    Thickness.THIN -> R.string.thickness_thin
    Thickness.NORMAL -> R.string.thickness_normal
    Thickness.THICK -> R.string.thickness_thick
    Thickness.UNKNOWN -> R.string.thickness_unknown
}

@StringRes
fun ColorGroup.labelResId(): Int = when (this) {
    ColorGroup.MONOTONE -> R.string.color_group_monotone
    ColorGroup.NAVY_BLUE -> R.string.color_group_navy_blue
    ColorGroup.VIVID -> R.string.color_group_vivid
    ColorGroup.EARTH_TONE -> R.string.color_group_earth_tone
    ColorGroup.PASTEL -> R.string.color_group_pastel
    ColorGroup.OTHER -> R.string.color_group_other
    ColorGroup.UNKNOWN -> R.string.color_group_unknown
}
