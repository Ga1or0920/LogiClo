package com.example.myapplication.domain.model

fun formatClothingDisplayLabel(
    categoryLabel: String,
    name: String,
    colorHex: String
): String {
    val colorCode = colorHex.takeIf { it.isNotBlank() }
        ?.removePrefix("#")
        ?.takeIf { it.isNotBlank() }
        ?.uppercase()
    return buildString {
        append("[")
        append(categoryLabel)
        append("] ")
        append(name)
        if (!colorCode.isNullOrBlank()) {
            append(" (#")
            append(colorCode)
            append(")")
        }
    }
}
