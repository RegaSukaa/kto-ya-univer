package com.kstoya.univer

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.kstoya.univer.data.CharacterEntity
import com.kstoya.univer.game.RoundInfo
import com.kstoya.univer.game.SummaryStats
import com.kstoya.univer.ui.components.CharacterCard
import com.kstoya.univer.ui.components.toCardModel
import com.kstoya.univer.ui.screens.ReviewScreen
import com.kstoya.univer.ui.screens.SummaryScreen
import com.kstoya.univer.ui.theme.KtoYaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Рендерит реальные экраны в PNG (1080×1920) без эмулятора. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "ru-rRU-w360dp-h640dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** Рисуем decorView в Bitmap: PixelCopy/hardware в Robolectric недоступен. */
    private fun save(name: String) {
        compose.waitForIdle()
        val view = compose.activity.window.decorView
        val w = if (view.width > 0) view.width else 1080
        val h = if (view.height > 0) view.height else 1920
        val image = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(image))
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private val friend = CharacterEntity(
        id = "friend_ivan", name = "Иван Петров", tag1 = "Группа ИТ-21",
        tag2 = "любитель поспать на парах", kind = "friend", bgPreset = 0,
        initials = "ИП", assetPhoto = "characters/friend_ivan.png", placeholder = true, source = "assets"
    )
    private val teacher = CharacterEntity(
        id = "teacher_smirnov", name = "Доцент Смирнов", tag1 = "Кафедра матана",
        tag2 = "строгий", kind = "teacher", bgPreset = 2,
        initials = "ДС", assetPhoto = "characters/teacher_smirnov.png", placeholder = true, source = "assets"
    )

    @Test
    fun cardFriend() {
        compose.setContent {
            KtoYaTheme(dark = true) {
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    CharacterCard(
                        card = friend.toCardModel(),
                        modifier = Modifier.fillMaxSize().padding(20.dp),
                        roundLabel = "Игрок 3 из 6"
                    )
                }
            }
        }
        save("s4_card_friend")
    }

    @Test
    fun cardTeacher() {
        compose.setContent {
            KtoYaTheme(dark = true) {
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    CharacterCard(
                        card = teacher.toCardModel(),
                        modifier = Modifier.fillMaxSize().padding(20.dp),
                        roundLabel = "Игрок 1 из 6"
                    )
                }
            }
        }
        save("s4_card_teacher")
    }

    @Test
    fun reviewScreen() {
        val rounds = listOf(
            RoundInfo("p0", "Рустам", "teacher_smirnov", "Доцент Смирнов"),
            RoundInfo("p1", "Аня", "friend_ivan", "Иван Петров"),
            RoundInfo("p2", "Дима", "friend_anya", "Аня Соколова"),
            RoundInfo("p3", "Соня", "friend_dima", "Дима Крылов"),
            RoundInfo("p4", "Марк", "teacher_kovalev", "Профессор Ковалёв"),
            RoundInfo("p5", "Тимур", "friend_lesha", "Лёша Морозов")
        )
        val chars = listOf(
            friend.copy(id = "friend_ivan", name = "Иван Петров"),
            teacher.copy(id = "teacher_smirnov"),
            friend.copy(id = "friend_anya", name = "Аня Соколова", initials = "АС", bgPreset = 3, assetPhoto = "characters/friend_anya.png"),
            friend.copy(id = "friend_dima", name = "Дима Крылов", initials = "ДК", bgPreset = 4, assetPhoto = "characters/friend_dima.png"),
            teacher.copy(id = "teacher_kovalev", name = "Профессор Ковалёв", initials = "ПК", bgPreset = 5, assetPhoto = "characters/teacher_kovalev.png"),
            friend.copy(id = "friend_lesha", name = "Лёша Морозов", initials = "ЛМ", bgPreset = 2, assetPhoto = "characters/friend_lesha.png")
        ).associateBy { it.id }
        val results = mapOf("p0" to "guessed", "p1" to "missed", "p2" to "guessed", "p3" to "skipped")
        compose.setContent {
            KtoYaTheme(dark = true) {
                ReviewScreen(
                    rounds = rounds, results = results, chars = chars,
                    onReshow = {}, onSetResult = { _, _ -> }, onNextRound = {}, onOpenSummary = {}, onExit = {}
                )
            }
        }
        save("s6_review")
    }

    @Test
    fun summaryScreen() {
        val rounds = listOf(
            RoundInfo("p0", "Рустам", "teacher_smirnov", "Доцент Смирнов"),
            RoundInfo("p1", "Аня", "friend_ivan", "Иван Петров"),
            RoundInfo("p2", "Дима", "friend_anya", "Аня Соколова"),
            RoundInfo("p3", "Соня", "friend_dima", "Дима Крылов"),
            RoundInfo("p4", "Марк", "teacher_kovalev", "Профессор Ковалёв"),
            RoundInfo("p5", "Тимур", "friend_lesha", "Лёша Морозов")
        )
        val chars = listOf(friend, teacher).associateBy { it.id }
        compose.setContent {
            KtoYaTheme(dark = true) {
                SummaryScreen(
                    rounds = rounds,
                    results = mapOf("p0" to "guessed", "p1" to "missed", "p2" to "guessed", "p3" to "guessed", "p4" to "missed", "p5" to "guessed"),
                    stats = SummaryStats(4, 6, listOf("Тимур"), 3, 4, 1, 2, 10),
                    scored = true,
                    chars = chars,
                    onNextRound = {}, onExit = {}
                )
            }
        }
        save("s8_summary")
    }
}
