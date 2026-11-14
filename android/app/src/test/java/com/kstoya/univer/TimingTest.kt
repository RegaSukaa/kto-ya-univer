package com.kstoya.univer

import com.kstoya.univer.game.LocalGameEngine
import com.kstoya.univer.game.LocalPhase
import com.kstoya.univer.game.PlayerSlot
import com.kstoya.univer.game.RoundInfo
import com.kstoya.univer.game.digitFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimingTest {
    private fun engine(players: Int = 2, showMs: Int = 10_000): LocalGameEngine {
        val rounds = (0 until players).map {
            RoundInfo("p$it", "Игрок $it", "c$it", "Персонаж $it")
        }
        val e = LocalGameEngine(rounds.map { PlayerSlot(it.playerId, it.playerName) }, rounds, showMs)
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
    fun `after last card the game goes to review`() {
        val e = engine(players = 1)
        e.ready(0)
        e.tick(12_800)
        assertTrue(e.finished)
        assertEquals(LocalPhase.REVIEW, e.phase)
    }

    @Test
    fun `cancel returns to pass and keeps the same player`() {
        val e = engine()
        e.ready(0)
        e.cancel(1_000)
        assertEquals(LocalPhase.PASS, e.phase)
        assertEquals(0, e.index)
    }

    @Test
    fun `reshow countdown uses the same 3-2-1 cadence`() {
        assertEquals(3, digitFor(0))
        assertEquals(2, digitFor(800))
        assertEquals(1, digitFor(1_600))
        assertEquals(0, digitFor(2_400))
        assertFalse(digitFor(2_800) == 3)
    }
}
