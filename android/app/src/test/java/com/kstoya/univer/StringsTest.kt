package com.kstoya.univer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class StringsTest {
    private fun keys(path: String): Set<String> {
        val text = File(path).readText()
        return Regex("<string name=\"([^\"]+)\"").findAll(text).map { it.groupValues[1] }.toSet()
    }

    @Test
    fun `ru and en have the same keys`() {
        val ru = keys("src/main/res/values/strings.xml")
        val en = keys("src/main/res/values-en/strings.xml")
        assertTrue("ru is empty", ru.size > 100)
        assertEquals("missing in en: ${ru - en}", emptySet<String>(), ru - en)
        assertEquals("missing in ru: ${en - ru}", emptySet<String>(), en - ru)
    }

    @Test
    fun `no hardcoded russian strings in screens`() {
        val dir = File("src/main/java/com/kstoya/univer/ui")
        val offenders = mutableListOf<String>()
        dir.walkTopDown().filter { it.extension == "kt" }.forEach { f ->
            f.readLines().forEachIndexed { i, line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("//") || trimmed.startsWith("*")) return@forEachIndexed
                val matches = Regex("\"([^\"]*[А-Яа-яЁё][^\"]*)\"").findAll(line)
                matches.forEach { m ->
                    // строки-образцы в Preview и тестах допустимы
                    if (!trimmed.startsWith("@Preview")) offenders += "${f.name}:${i + 1}: ${m.value}"
                }
            }
        }
        assertTrue("Русский текст в коде: $offenders", offenders.isEmpty())
    }
}
