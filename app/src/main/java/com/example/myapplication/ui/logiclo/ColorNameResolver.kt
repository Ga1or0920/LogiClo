package com.example.myapplication.ui.logiclo

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object ColorNameResolver {
    /**
     * HEX (#RRGGBB or #AARRGGBB) から簡易的な色名を返す。
     * 完璧な分類ではなく、代表色（レッド/ブルー/ネイビー/グリーン/ブラック/ホワイト/グレー/ベージュ/ブラウン/ピンク/パープル/イエロー/オレンジ）を返す。
     */
    fun resolve(hex: String): String {
        return try {
            val colorInt = android.graphics.Color.parseColor(hex)
            val r = (colorInt shr 16 and 0xFF) / 255.0
            val g = (colorInt shr 8 and 0xFF) / 255.0
            val b = (colorInt and 0xFF) / 255.0

            val maxc = max(max(r, g), b)
            val minc = min(min(r, g), b)
            val l = (maxc + minc) / 2.0
            val delta = maxc - minc

            val s = if (delta == 0.0) 0.0 else delta / (1.0 - abs(2.0 * l - 1.0))

            var h = 0.0
            if (delta != 0.0) {
                h = when (maxc) {
                    r -> ((g - b) / delta) % 6.0
                    g -> ((b - r) / delta) + 2.0
                    else -> ((r - g) / delta) + 4.0
                }
                h *= 60.0
                if (h < 0) h += 360.0
            }

            val sat = s
            val light = l

            if (light < 0.12) return "ブラック"
            if (light > 0.92) return "ホワイト"

            if (sat < 0.12) {
                // 彩度が低い場合はグレートーンやベージュ/ブラウンを判定
                return when {
                    h in 20.0..45.0 -> "ベージュ"
                    h in 10.0..40.0 -> "ブラウン"
                    else -> "グレー"
                }
            }

            return when (h) {
                in 345.0..360.0, in 0.0..15.0 -> "レッド"
                in 15.0..45.0 -> "オレンジ"
                in 45.0..70.0 -> "イエロー"
                in 70.0..165.0 -> "グリーン"
                in 165.0..210.0 -> "シアン"
                in 210.0..260.0 -> {
                    if (light < 0.35) "ネイビー" else "ブルー"
                }
                in 260.0..320.0 -> {
                    if (sat > 0.6) "パープル" else "ピンク"
                }
                else -> "その他"
            }
        } catch (e: Exception) {
            ""
        }
    }
}
