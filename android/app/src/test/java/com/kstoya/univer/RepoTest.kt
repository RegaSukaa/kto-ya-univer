package com.kstoya.univer

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kstoya.univer.data.AppDb
import com.kstoya.univer.data.CharacterEntity
import com.kstoya.univer.data.Prefs
import com.kstoya.univer.data.Repo
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RepoTest {
    private lateinit var ctx: Context
    private lateinit var db: AppDb
    private lateinit var repo: Repo

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDb::class.java).allowMainThreadQueries().build()
        repo = Repo(ctx, db, Prefs(ctx))
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun sample(id: String = "c1", photo: String? = null) = CharacterEntity(
        id = id, name = "Аня", tag1 = "Староста", kind = "friend", initials = "А",
        photoPath = photo, createdAt = 1L, updatedAt = 1L
    )

    @Test
    fun `character crud`() = runBlocking {
        repo.saveCharacter(sample())
        assertEquals(1, repo.allCharacters().size)
        assertEquals("Аня", repo.character("c1")?.name)
        repo.saveCharacter(sample().copy(name = "Аня Соколова"))
        assertEquals("Аня Соколова", repo.character("c1")?.name)
        repo.deleteCharacter(sample())
        assertNull(repo.character("c1"))
    }

    @Test
    fun `deleting a character removes its photo file`() = runBlocking {
        val f = File(ctx.filesDir, "cards/c1_cut.webp").apply { parentFile?.mkdirs(); writeText("x") }
        repo.saveCharacter(sample(photo = f.absolutePath))
        assertTrue(f.exists())
        repo.deleteCharacter(sample(photo = f.absolutePath))
        assertFalse(f.exists())
    }

    @Test
    fun `wipe clears database and files`() = runBlocking {
        val cards = File(ctx.filesDir, "cards").apply { mkdirs() }
        File(cards, "c1_cut.webp").writeText("x")
        val voice = File(ctx.filesDir, "voice").apply { mkdirs() }
        File(voice, "start.m4a").writeText("x")
        repo.saveCharacter(sample(photo = File(cards, "c1_cut.webp").absolutePath))
        repo.savePreset("ИТ-21", listOf("Аня", "Дима"))

        repo.wipeAll()

        assertEquals(0, repo.allCharacters().size)
        assertTrue(repo.presets().isEmpty())
        assertEquals(0, File(ctx.filesDir, "cards").listFiles()?.size ?: 0)
        assertEquals(0, File(ctx.filesDir, "voice").listFiles()?.size ?: 0)
    }

    @Test
    fun `presets keep names in order`() = runBlocking {
        repo.savePreset("ИТ-21", listOf("Аня", "Дима", "Соня"))
        val p = repo.latestPreset()!!
        assertEquals(listOf("Аня", "Дима", "Соня"), repo.presetNames(p))
    }

    @Test
    fun `session rounds and results are stored`() = runBlocking {
        val id = repo.createSession("local", 2, 10000, 42L, "sha256:x")
        repo.addRound(id, 0, com.kstoya.univer.game.PlayerSlot("p0", "Аня"), "c1", "Аня", 10000)
        repo.setResult(id, "p0", "guessed")
        val rounds = db.sessionDao().rounds(id)
        assertEquals(1, rounds.size)
        assertEquals("guessed", rounds.first().result)
    }
}
