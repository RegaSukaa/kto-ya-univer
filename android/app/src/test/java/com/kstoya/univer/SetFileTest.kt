package com.kstoya.univer

import androidx.test.core.app.ApplicationProvider
import com.kstoya.univer.data.CharacterEntity
import com.kstoya.univer.data.SetFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SetFileTest {
    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun sample(): List<CharacterEntity> = listOf(
        CharacterEntity(id = "c1", name = "Виктор Павлович", tag1 = "Доцент кафедры матана", tag2 = "строгий", kind = "teacher", bgPreset = 2, initials = "ВП"),
        CharacterEntity(id = "c2", name = "Аня", tag1 = "Староста", kind = "friend", bgPreset = 0, initials = "А")
    )

    @Test
    fun `export and import keep the deck hash`() {
        val setFile = SetFile(ctx)
        val chars = sample()
        val out = ByteArrayOutputStream()
        assertEquals(2, setFile.writeSet(chars, "ИТ-21", out))
        val read = setFile.readSet(ByteArrayInputStream(out.toByteArray()))
        assertEquals("ktya/1", read.manifest.format)
        assertEquals(2, read.manifest.characters.size)
        assertEquals(setFile.deckHash(chars), read.manifest.deckHash)
    }

    @Test
    fun `hash is order independent`() {
        val setFile = SetFile(ctx)
        val chars = sample()
        assertEquals(setFile.deckHash(chars), setFile.deckHash(chars.reversed()))
    }

    @Test
    fun `broken zip fails with an exception, not a crash`() {
        val setFile = SetFile(ctx)
        val bad = ByteArrayInputStream(byteArrayOf(1, 2, 3, 4, 5))
        var failed: Exception? = null
        try {
            setFile.readSet(bad)
        } catch (e: Exception) {
            failed = e
        }
        assertNotNull("импорт битого файла должен бросать понятную ошибку", failed)
    }
}
