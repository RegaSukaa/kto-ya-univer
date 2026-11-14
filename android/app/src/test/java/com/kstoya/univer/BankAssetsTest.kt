package com.kstoya.univer

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kstoya.univer.data.AppDb
import com.kstoya.univer.data.CharacterEntity
import com.kstoya.univer.data.Prefs
import com.kstoya.univer.data.Repo
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BankAssetsTest {
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
    fun tearDown() = db.close()

    private fun manifest(): JSONObject {
        val text = ctx.assets.open("characters.json").bufferedReader().use { it.readText() }
        return JSONObject(text)
    }

    private fun manifestPeople(): List<JSONObject> {
        val arr = manifest().getJSONArray("characters")
        return (0 until arr.length()).map { arr.getJSONObject(it) }
    }

    @Test
    fun `manifest is valid and every person has a photo`() {
        val people = manifestPeople()
        assertTrue("в банке должно быть минимум 2 человека", people.size >= 2)
        assertEquals(people.size, people.map { it.getString("id") }.toSet().size)
        people.forEach { p ->
            val photos = p.optJSONArray("photos")
            assertNotNull("${p.getString("id")} без photos[]", photos)
            assertTrue("${p.getString("id")} без фото", (photos?.length() ?: 0) >= 1)
            assertTrue("${p.getString("name")} без имени", p.getString("name").isNotBlank())
        }
    }

    @Test
    fun `every photo file exists in assets and is not empty`() {
        var checked = 0
        manifestPeople().forEach { p ->
            val arr = p.getJSONArray("photos")
            for (i in 0 until arr.length()) {
                val path = arr.getString(i)
                val size = ctx.assets.open(path).use { it.readBytes().size }
                assertTrue("пустой файл $path", size > 1000)
                checked++
            }
        }
        assertTrue("проверено слишком мало фото: $checked", checked >= 20)
    }

    @Test
    fun `seeding fills the database with the whole bank`() = runBlocking {
        val expected = manifestPeople().size
        assertEquals(expected, repo.seedFromAssets())
        val all = repo.allCharacters()
        assertEquals(expected, all.size)
        all.forEach { c ->
            assertNotNull("${c.id} без фото", c.assetPhoto)
            assertTrue("${c.id} без инициалов", c.initials.isNotBlank())
        }
    }

    @Test
    fun `seeding twice does not duplicate`() = runBlocking {
        repo.seedFromAssets()
        repo.seedFromAssets()
        assertEquals(manifestPeople().size, repo.allCharacters().size)
    }

    @Test
    fun `stale placeholder from an older APK is removed`() = runBlocking {
        repo.saveCharacter(
            CharacterEntity(
                id = "friend_ivan", name = "Иван Петров", kind = "friend",
                assetPhoto = "characters/friend_ivan.png", source = "assets", placeholder = true
            )
        )
        repo.seedFromAssets()
        org.junit.Assert.assertNull(repo.character("friend_ivan"))
    }

    @Test
    fun `user cannot break the game by deleting everyone`() = runBlocking {
        repo.seedFromAssets()
        db.characterDao().deleteAll()
        assertEquals(0, repo.allCharacters().size)
        repo.seedFromAssets()
        assertEquals(manifestPeople().size, repo.allCharacters().size)
    }

    @Test
    fun `voice bank and built-in sounds exist`() {
        val voices = ctx.assets.list("voices/go")?.filter { it.endsWith(".m4a") || it.endsWith(".wav") } ?: emptyList()
        assertTrue("нет голосов go", voices.isNotEmpty())
        val list = ctx.assets.list("sounds")?.toSet() ?: emptySet()
        listOf("go.wav", "tick.wav", "time_up.wav", "win.wav", "skip.wav", "click.wav").forEach {
            assertTrue("нет $it", list.contains(it))
        }
    }
}
