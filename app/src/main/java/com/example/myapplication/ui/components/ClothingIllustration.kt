package com.example.myapplication.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.domain.model.ClothingCategory

@Composable
fun ClothingIllustrationSwatch(
    category: ClothingCategory,
    colorHex: String,
    modifier: Modifier = Modifier,
    swatchSize: Dp = 56.dp,
    iconSize: Dp = 36.dp
) {
    val tint = runCatching { Color(android.graphics.Color.parseColor(colorHex)) }
        .getOrElse { MaterialTheme.colorScheme.primary }
    Box(
        modifier = modifier
            .size(swatchSize)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        ClothingIllustration(
            category = category,
            tint = tint,
            modifier = Modifier
                .align(Alignment.Center)
                .size(iconSize)
        )
    }
}

@Composable
fun ClothingIllustration(
    category: ClothingCategory,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val painter = painterResource(category.toDrawableRes())
    Image(
        painter = painter,
        contentDescription = null,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier
    )
}

@DrawableRes
private fun ClothingCategory.toDrawableRes(): Int = when (this) {
    ClothingCategory.T_SHIRT,
    ClothingCategory.POLO,
    ClothingCategory.DRESS_SHIRT,
    ClothingCategory.KNIT,
    ClothingCategory.SWEATSHIRT,
    ClothingCategory.JACKET -> R.drawable.ic_clothing_top

    ClothingCategory.DENIM,
    ClothingCategory.SLACKS,
    ClothingCategory.CHINO -> R.drawable.ic_clothing_bottom

    ClothingCategory.OUTER_LIGHT,
    ClothingCategory.COAT -> R.drawable.ic_clothing_outer

    ClothingCategory.INNER -> R.drawable.ic_clothing_inner

    ClothingCategory.UNKNOWN -> R.drawable.ic_clothing_top
}
