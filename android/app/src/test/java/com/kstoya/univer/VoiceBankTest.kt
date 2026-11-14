package com.kstoya.univer

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.kstoya.univer.audio.Sounds
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class VoiceBankTest {
    @Test
    fun `go voice bank is present and playable`() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val files = ctx.assets.list("voices/go")?.filter { it.endsWith(".m4a") || it.endsWith(".wav") } ?: emptyList()
        assertTrue("в assets/voices/go нет файлов", files.size >= 1)
        assertTrue(Sounds(ctx).hasBank("go"))
    }
}
