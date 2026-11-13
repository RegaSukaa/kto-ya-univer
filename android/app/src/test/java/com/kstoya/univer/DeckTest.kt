package com.kstoya.univer

import com.kstoya.univer.game.Deck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DeckTest {
    private val kinds = mutableMapOf<String, String>()
    private val deck: List<String> = buildList {
        repeat(6) { add("f$it"); kinds["f$it"] = "friend" }
        repeat(4) { add("t$it"); kinds["t$it"] = "teacher" }
    }

    private fun deal(seed: Long, players: Int, alternate: Boolean = true) =
        Deck.deal(seed, deck, (0 until players).map { "p$it" }, alternate) { kinds[it] ?: "other" }

    @Test
    fun `no repeats when deck is big enough`() {
        val result = deal(42, 8)
        assertEquals(8, result.size)
        assertEquals(8, result.values.toSet().size)
    }

    @Test
    fun `repeats allowed but never adjacent when deck is small`() {
        val result = deal(7, 10)
        val order = (0 until 10).map { result["p$it"] }
        for (i in 1 until order.size) assertNotEquals(order[i - 1], order[i])
    }

    @Test
    fun `deal is deterministic`() {
        assertEquals(deal(42, 6), deal(42, 6))
        assertNotEquals(deal(42, 6), deal(43, 6))
    }

    @Test
    fun `types alternate at least 80 percent`() {
        val result = deal(123, 8)
        val kindsSeq = (0 until 8).map { kinds[result["p$it"]] }
        val transitions = (1 until kindsSeq.size).count { kindsSeq[it] != kindsSeq[it - 1] }
        val ratio = transitions.toDouble() / (kindsSeq.size - 1)
        assertFalse("ratio=$ratio", ratio < 0.8)
    }

    @Test
    fun `every player gets exactly one character`() {
        val result = deal(1, 5)
        assertEquals(5, result.size)
        assertEquals((0 until 5).map { "p$it" }.toSet(), result.keys)
    }

    @Test
    fun `empty inputs do not crash`() {
        assertEquals(emptyMap<String, String>(), Deck.deal(1, emptyList(), listOf("p"), true) { "other" })
        assertEquals(emptyMap<String, String>(), Deck.deal(1, listOf("a"), emptyList(), true) { "other" })
    }
}
