package com.example.myapplication.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.myapplication.domain.model.ClothingCategory
import kotlin.math.min

@Composable
fun ClothingIllustrationSwatch(
    category: ClothingCategory,
    colorHex: String,
    modifier: Modifier = Modifier,
    swatchSize: Dp = 56.dp,
    iconSize: Dp = 36.dp
) {
    val fillColor = runCatching { Color(android.graphics.Color.parseColor(colorHex)) }
        .getOrElse { MaterialTheme.colorScheme.primary }
    val shape = RoundedCornerShape(16.dp)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDarkSurface = surfaceColor.luminance() < 0.5f
    val borderColor = if (isDarkSurface) Color.White else Color.Black
    val outlineColor = if (fillColor.luminance() < 0.5f) Color.White else Color.Black

    Surface(
        modifier = modifier.size(swatchSize),
        shape = shape,
        border = BorderStroke(2.dp, borderColor),
        color = surfaceColor
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            ClothingIllustrationGlyph(
                category = category,
                fillColor = fillColor,
                strokeColor = outlineColor,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
private fun ClothingIllustrationGlyph(
    category: ClothingCategory,
    fillColor: Color,
    strokeColor: Color,
    modifier: Modifier = Modifier
) {
    val pathData = remember(category) { clothingPathData(category) }
    val fillPaths = remember(pathData) { pathData.fillAndStroke }
    val strokeOnlyPaths = remember(pathData) { pathData.strokeOnly }

    Canvas(modifier = modifier) {
        val minDimension = min(size.width, size.height)
        if (minDimension <= 0f) return@Canvas
        val scaleFactor = minDimension / VIEWPORT_SIZE
        val translateX = (size.width - VIEWPORT_SIZE * scaleFactor) / 2f
        val translateY = (size.height - VIEWPORT_SIZE * scaleFactor) / 2f

        withTransform({
            scale(scaleX = scaleFactor, scaleY = scaleFactor, pivot = Offset.Zero)
            translate(left = translateX, top = translateY)
        }) {
            fillPaths.forEach { path ->
                drawPath(path = path, color = fillColor, style = Fill)
                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(
                        width = STROKE_WIDTH,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
            strokeOnlyPaths.forEach { path ->
                drawPath(
                    path = path,
                    color = strokeColor,
                    style = Stroke(
                        width = STROKE_WIDTH,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}

private data class ClothingPathData(
    val fillAndStroke: List<Path>,
    val strokeOnly: List<Path> = emptyList()
)

private const val VIEWPORT_SIZE = 24f
private const val STROKE_WIDTH = 1.5f

private fun clothingPathData(category: ClothingCategory): ClothingPathData = when (category) {
    ClothingCategory.T_SHIRT,
    ClothingCategory.POLO,
    ClothingCategory.DRESS_SHIRT,
    ClothingCategory.KNIT,
    ClothingCategory.SWEATSHIRT,
    ClothingCategory.JACKET,
    ClothingCategory.OUTER_LIGHT,
    ClothingCategory.DOWN,
    ClothingCategory.COAT,
    ClothingCategory.UNKNOWN -> ClothingPathData(
        fillAndStroke = listOf(buildTopPath())
    )

    ClothingCategory.DENIM,
    ClothingCategory.SLACKS,
    ClothingCategory.CHINO -> ClothingPathData(
        fillAndStroke = listOf(buildBottomsPath()),
        strokeOnly = listOf(buildBottomsCenterSeamPath())
    )

    ClothingCategory.INNER -> ClothingPathData(
        fillAndStroke = listOf(buildInnerPath())
    )
}

private fun buildTopPath(): Path = Path().apply {
    moveTo(4f, 5f)
    lineTo(8f, 5f)
    lineTo(8f, 3f)
    lineTo(16f, 3f)
    lineTo(16f, 5f)
    lineTo(20f, 5f)
    lineTo(20f, 11f)
    lineTo(16f, 11f)
    lineTo(16f, 21f)
    lineTo(8f, 21f)
    lineTo(8f, 11f)
    lineTo(4f, 11f)
    close()
}

private fun buildBottomsPath(): Path = Path().apply {
    moveTo(7f, 4f)
    lineTo(17f, 4f)
    lineTo(18.5f, 20f)
    lineTo(14.5f, 20f)
    lineTo(13.5f, 12.5f)
    lineTo(10.5f, 12.5f)
    lineTo(9.5f, 20f)
    lineTo(5.5f, 20f)
    close()
}

private fun buildBottomsCenterSeamPath(): Path = Path().apply {
    moveTo(12f, 4f)
    lineTo(12f, 8f)
}

private fun buildInnerPath(): Path = Path().apply {
    moveTo(8f, 4f)
    lineTo(8f, 7f)
    lineTo(7f, 8f)
    lineTo(7f, 21f)
    lineTo(17f, 21f)
    lineTo(17f, 8f)
    lineTo(16f, 7f)
    lineTo(16f, 4f)
    lineTo(13f, 4f)
    lineTo(13f, 7f)
    lineTo(11f, 7f)
    lineTo(11f, 4f)
    close()
}
