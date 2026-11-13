package com.kstoya.univer.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Bg = Color(0xFF101014)
val Surface = Color(0xFF1A1A21)
val Accent = Color(0xFFFFB020)
val AccentLight = Color(0xFFF59300)
val TextMain = Color(0xFFF7F7FA)
val Muted = Color(0xFF9AA0AA)

data class CardGradient(val top: Color, val bottom: Color) {
    fun brush(): Brush = Brush.linearGradient(listOf(top, bottom))
}

/** 6 пресетов фона карточки: синий, красный, изумрудный, фиолетовый, янтарный, графит. */
val CardPresets: List<CardGradient> = listOf(
    CardGradient(Color(0xFF2563EB), Color(0xFF1E40AF)),
    CardGradient(Color(0xFFDC2626), Color(0xFF991B1B)),
    CardGradient(Color(0xFF047857), Color(0xFF064E3B)),
    CardGradient(Color(0xFF7C3AED), Color(0xFF5B21B6)),
    CardGradient(Color(0xFFB45309), Color(0xFF7C2D12)),
    CardGradient(Color(0xFF334155), Color(0xFF1E293B))
)

fun defaultPresetFor(kind: String): Int = when (kind) {
    "friend" -> 0
    "teacher" -> 1
    else -> 5
}

fun presetFor(kind: String, bgPreset: Int): CardGradient = CardPresets[bgPreset.coerceIn(0, 5)]

/** Контраст белого текста на цвете (WCAG). */
fun contrastWithWhite(color: Color): Double {
    fun lum(c: Color): Double {
        fun ch(v: Float): Double {
            val s = v.toDouble()
            return if (s <= 0.03928) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * ch(c.red) + 0.7152 * ch(c.green) + 0.0722 * ch(c.blue)
    }
    val l1 = 1.0
    val l2 = lum(color)
    return (l1 + 0.05) / (l2 + 0.05)
}
