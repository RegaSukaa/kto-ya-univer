package com.kstoya.univer

import com.kstoya.univer.audio.SoundPriority
import com.kstoya.univer.audio.SoundSource
import org.junit.Assert.assertEquals
import org.junit.Test

class SoundPriorityTest {
    @Test
    fun `voice bank wins over built-in tone`() {
        assertEquals(SoundSource.BANK, SoundPriority.resolve(true))
    }

    @Test
    fun `never silence`() {
        assertEquals(SoundSource.ASSET, SoundPriority.resolve(false))
    }
}
