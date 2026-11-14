package com.kstoya.univer.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun initialsOf(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val first = parts.firstOrNull()?.firstOrNull()?.uppercase() ?: "?"
    val second = parts.getOrNull(1)?.firstOrNull()?.uppercase()
    return if (second != null) "$first$second" else first
}

fun tagLine(vararg tags: String?): String = tags.filterNotNull().filter { it.isNotBlank() }.joinToString(" · ")

fun dateShort(ms: Long): String = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(ms))

fun shareText(title: String, text: String): String = "$title\n$text"

/** Индекс выбранного времени показа: 5 с — 0, 10 с — 1 и т.д. */
fun showTimeIndex(showMs: Int, times: List<Int> = listOf(5, 10, 15, 20, 30)): Int =
    times.indexOfFirst { it * 1000 == showMs }.coerceAtLeast(0)
