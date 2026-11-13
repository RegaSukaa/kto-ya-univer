package com.kstoya.univer

import com.kstoya.univer.audio.SoundPriority
import com.kstoya.univer.audio.SoundSource
import org.junit.Assert.assertEquals
import org.junit.Test

class SoundPriorityTest {
    @Test
    fun `personal voice wins`() {
        assertEquals(SoundSource.PERSONAL, SoundPriority.resolve(true, true))
        assertEquals(SoundSource.PERSONAL, SoundPriority.resolve(true, false))
    }

    @Test
    fun `voice bank is second`() {
        assertEquals(SoundSource.BANK, SoundPriority.resolve(false, true))
    }

    @Test
    fun `never silence`() {
        assertEquals(SoundSource.ASSET, SoundPriority.resolve(false, false))
    }
}
