package com.kstoya.univer

import com.kstoya.univer.game.LocalGameEngine
import com.kstoya.univer.game.LocalPhase
import com.kstoya.univer.game.PlayerSlot
import com.kstoya.univer.game.RoundInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimingTest {
    private fun engine(players: Int = 2, showMs: Int = 10_000, markAfter: Boolean = false): LocalGameEngine {
        val rounds = (0 until players).map {
            RoundInfo("p$it", "Игрок $it", "c$it", "Персонаж $it")
        }
        val e = LocalGameEngine(rounds.map { PlayerSlot(it.playerId, it.playerName) }, rounds, showMs, markAfter)
        e.start(0)
        return e
    }

    @Test
    fun `phase boundaries at 0 2800 12800`() {
        val e = engine()
        assertEquals(LocalPhase.PASS, e.phase)
        e.ready(0)
        assertEquals(LocalPhase.COUNTDOWN, e.phase)
        e.tick(2_799)
        assertEquals(LocalPhase.COUNTDOWN, e.phase)
        e.tick(2_800)
        assertEquals(LocalPhase.SHOW, e.phase)
        assertEquals(10_000L, e.remaining(2_800))
        e.tick(12_799)
        assertEquals(LocalPhase.SHOW, e.phase)
        e.tick(12_800)
        assertEquals(LocalPhase.PASS, e.phase)
        assertEquals(1, e.index)
    }

    @Test
    fun `showMs five seconds ends at 7800`() {
        val e = engine(showMs = 5_000)
        e.ready(0)
        e.tick(2_800)
        assertEquals(LocalPhase.SHOW, e.phase)
        e.tick(7_799)
        assertEquals(LocalPhase.SHOW, e.phase)
        e.tick(7_800)
        assertEquals(1, e.index)
    }

    @Test
    fun `countdown digits 3 2 1 0`() {
        val e = engine()
        e.ready(0)
        assertEquals(3, e.countdownDigit(0))
        assertEquals(2, e.countdownDigit(800))
        assertEquals(1, e.countdownDigit(1_600))
        assertEquals(0, e.countdownDigit(2_400))
    }

    @Test
    fun `marking is required when markAfter is on`() {
        val e = engine(markAfter = true)
        e.ready(0)
        e.tick(12_800)
        assertEquals(LocalPhase.MARK, e.phase)
        e.mark(13_000, "guessed")
        assertEquals(LocalPhase.PASS, e.phase)
        assertEquals(1, e.index)
        assertEquals("guessed", e.results["p0"])
    }

    @Test
    fun `last round goes to review`() {
        val e = engine(players = 1)
        e.ready(0)
        e.tick(12_800)
        assertTrue(e.finished)
        assertEquals(LocalPhase.REVIEW, e.phase)
        assertFalse(e.phase == LocalPhase.PASS)
    }

    @Test
    fun `skipping marks skipped and advances`() {
        val e = engine(players = 2, markAfter = true)
        e.ready(0)
        e.tick(12_800)
        e.skipRound(12_900)
        assertEquals("skipped", e.results["p0"])
        assertEquals(1, e.index)
    }

    @Test
    fun `cancel returns to pass and keeps the same player`() {
        val e = engine()
        e.ready(0)
        e.cancel(1_000)
        assertEquals(LocalPhase.PASS, e.phase)
        assertEquals(0, e.index)
    }
}
