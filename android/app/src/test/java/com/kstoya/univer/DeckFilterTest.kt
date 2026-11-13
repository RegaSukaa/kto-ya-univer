package com.kstoya.univer

import com.kstoya.univer.game.DeckFilter
import org.junit.Assert.assertEquals
import org.junit.Test

class DeckFilterTest {
    private data class P(val id: String, val kind: String)
    private val all = listOf(
        P("f1", "friend"), P("f2", "friend"), P("f3", "friend"),
        P("t1", "teacher"), P("t2", "teacher"),
        P("o1", "other")
    )

    @Test
    fun `all keeps everyone`() {
        assertEquals(all, DeckFilter.apply(all, DeckFilter.ALL) { it.kind })
    }

    @Test
    fun `students only`() {
        assertEquals(listOf("f1", "f2", "f3"), DeckFilter.apply(all, DeckFilter.FRIENDS) { it.kind }.map { it.id })
    }

    @Test
    fun `teachers only`() {
        assertEquals(listOf("t1", "t2"), DeckFilter.apply(all, DeckFilter.TEACHERS) { it.kind }.map { it.id })
    }

    @Test
    fun `unknown filter behaves like all`() {
        assertEquals(all.size, DeckFilter.apply(all, "nonsense") { it.kind }.size)
    }
}
