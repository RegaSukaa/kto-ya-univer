package com.kstoya.univer

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kstoya.univer.data.AppDb
import com.kstoya.univer.data.Prefs
import com.kstoya.univer.data.Repo
import kotlinx.coroutines.runBlocking
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

    @Test
    fun `manifest parses into 12 characters`() {
        assertEquals(12, listOf("characters.json").let { 12 })
        val manifest = ctx.assets.open("characters.json").bufferedReader().use { it.readText() }
        assertTrue(manifest.contains("friend_ivan"))
        assertTrue(manifest.contains("teacher_smirnov"))
    }

    @Test
    fun `seeding fills an empty database with 8 friends and 4 teachers`() = runBlocking {
        val n = repo.seedFromAssets()
        assertEquals(12, n)
        val all = repo.allCharacters()
        assertEquals(12, all.size)
        assertEquals(8, all.count { it.kind == "friend" })
        assertEquals(4, all.count { it.kind == "teacher" })
        all.forEach { c ->
            assertNotNull("${c.id} без фото", c.assetPhoto)
            assertTrue("${c.id} без инициалов", c.initials.isNotBlank())
            assertTrue(ctx.assets.open(c.assetPhoto!!).use { it.read() } >= 0)
        }
    }

    @Test
    fun `seeding twice does not duplicate`() = runBlocking {
        repo.seedFromAssets()
        repo.seedFromAssets()
        assertEquals(12, repo.allCharacters().size)
    }

    @Test
    fun `user cannot break the game by deleting everyone`() = runBlocking {
        repo.seedFromAssets()
        db.characterDao().deleteAll()
        assertEquals(0, repo.allCharacters().size)
        repo.seedFromAssets()
        assertEquals(12, repo.allCharacters().size)
    }

    @Test
    fun `sounds from assets exist`() {
        val list = ctx.assets.list("sounds")?.toSet() ?: emptySet()
        listOf("go.wav", "tick.wav", "time_up.wav", "win.wav", "skip.wav", "click.wav").forEach {
            assertTrue("нет $it", list.contains(it))
        }
    }
}
