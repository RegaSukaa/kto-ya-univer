package com.kstoya.univer

import com.kstoya.univer.ui.theme.CardPresets
import com.kstoya.univer.ui.theme.contrastWithWhite
import org.junit.Assert.assertTrue
import org.junit.Test

class ContrastTest {
    @Test
    fun `white text is readable on every preset`() {
        CardPresets.forEachIndexed { i, preset ->
            val top = contrastWithWhite(preset.top)
            val bottom = contrastWithWhite(preset.bottom)
            assertTrue("preset $i top contrast=$top", top >= 4.5)
            assertTrue("preset $i bottom contrast=$bottom", bottom >= 4.5)
        }
    }
}
