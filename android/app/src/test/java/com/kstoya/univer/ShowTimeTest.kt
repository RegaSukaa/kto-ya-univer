package com.kstoya.univer

import com.kstoya.univer.util.showTimeIndex
import org.junit.Assert.assertEquals
import org.junit.Test

class ShowTimeTest {
    @Test
    fun `five seconds is the first chip`() {
        assertEquals(0, showTimeIndex(5_000))
        assertEquals(1, showTimeIndex(10_000))
        assertEquals(2, showTimeIndex(15_000))
        assertEquals(3, showTimeIndex(20_000))
        assertEquals(4, showTimeIndex(30_000))
    }

    @Test
    fun `unknown value falls back to the first chip`() {
        assertEquals(0, showTimeIndex(7_000))
    }
}
