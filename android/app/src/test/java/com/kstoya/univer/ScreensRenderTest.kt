package com.kstoya.univer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.kstoya.univer.data.CharacterEntity
import com.kstoya.univer.data.Settings
import com.kstoya.univer.game.RoundInfo
import com.kstoya.univer.game.SummaryStats
import com.kstoya.univer.ui.components.CharacterCard
import com.kstoya.univer.ui.components.fallbackCard
import com.kstoya.univer.ui.components.toCardModel
import com.kstoya.univer.ui.screens.HomeScreen
import com.kstoya.univer.ui.screens.ReviewScreen
import com.kstoya.univer.ui.screens.SetupScreen
import com.kstoya.univer.ui.screens.SummaryScreen
import com.kstoya.univer.ui.theme.KtoYaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "ru-rRU-w411dp-h891dp-xxhdpi")
class ScreensRenderTest {
    @get:Rule
    val compose = createComposeRule()

    private fun chars(n: Int = 6): List<CharacterEntity> = (0 until n).map {
        CharacterEntity(
            id = "c$it", name = "Персонаж $it", tag1 = "Группа ИТ-21", tag2 = "тег два",
            tag3 = "тег три", kind = if (it % 2 == 0) "friend" else "teacher",
            bgPreset = it % 6, initials = "П$it", createdAt = it.toLong(), updatedAt = 1L
        )
    }

    @Test
    fun homeShowsLocalGameButton() {
        compose.setContent {
            KtoYaTheme { HomeScreen(characters = chars(3), onLocal = {}, onOnline = {}, onLibrary = {}, onSettings = {}) }
        }
        compose.onNodeWithText(ApplicationProvider.getApplicationContext<android.content.Context>().getString(R.string.home_local)).assertExists()
    }

    @Test
    fun setupRendersWithTwoAndTenPlayers() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        compose.setContent {
            KtoYaTheme {
                SetupScreen(
                    characters = chars(12),
                    settings = Settings(),
                    presets = emptyList(),
                    presetNames = { emptyList() },
                    onBack = {},
                    onSavePreset = { _, _ -> },
                    onStart = { _, _, _, _ -> },
                    onAddCharacters = {}
                )
            }
        }
        compose.onNodeWithText(ctx.getString(R.string.setup_start)).assertExists()
        compose.onNodeWithText(ctx.getString(R.string.setup_show_time)).assertExists()
    }

    @Test
    fun cardWithLongNameAndThreeTagsFits() {
        val long = chars(1).first().copy(name = "Виктор Павлович Смирнов-Заречный", tag1 = "Доцент кафедры матана", tag2 = "строгий", tag3 = "ставит зачёт по списку")
        compose.setContent {
            KtoYaTheme {
                Box(Modifier.fillMaxWidth().height(560.dp).padding(16.dp)) {
                    CharacterCard(card = long.toCardModel(), modifier = Modifier.fillMaxWidth().height(520.dp))
                }
            }
        }
        compose.onNodeWithText("Виктор Павлович Смирнов-Заречный").assertExists()
        compose.onNodeWithText("Доцент кафедры матана · строгий · ставит зачёт по списку").assertExists()
    }

    @Test
    fun cardWithoutPhotoShowsInitials() {
        compose.setContent {
            KtoYaTheme {
                Box(Modifier.fillMaxWidth().height(420.dp)) {
                    CharacterCard(card = fallbackCard("Иван Петров"), modifier = Modifier.fillMaxWidth().height(400.dp))
                }
            }
        }
        compose.onNodeWithText("ИП").assertExists()
    }

    @Test
    fun reviewShowsSixPlayersAndStatuses() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val rounds = (0 until 6).map { RoundInfo("p$it", "Игрок $it", "c$it", "Персонаж $it") }
        val results = mapOf("p0" to "guessed", "p1" to "missed", "p2" to "skipped")
        compose.setContent {
            KtoYaTheme {
                ReviewScreen(
                    rounds = rounds,
                    results = results,
                    chars = chars(6).associateBy { it.id },
                    onReshow = {}, onSetResult = { _, _ -> }, onNextRound = {}, onOpenSummary = {}, onExit = {}
                )
            }
        }
        (0 until 6).forEach { compose.onNodeWithText("Игрок $it").assertExists() }
        compose.onNodeWithText("✓ " + ctx.getString(R.string.review_status_guessed)).assertExists()
        compose.onNodeWithText(ctx.getString(R.string.review_finish)).assertExists()
    }

    @Test
    fun summaryShowsScore() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val rounds = (0 until 6).map { RoundInfo("p$it", "Игрок $it", "c$it", "Персонаж $it") }
        val results = (0 until 4).associate { "p$it" to "guessed" }
        compose.setContent {
            KtoYaTheme {
                SummaryScreen(
                    rounds = rounds,
                    results = results,
                    stats = SummaryStats(4, 6, listOf("Игрок 0"), 3, 3, 1, 3, 10),
                    scored = true,
                    chars = chars(6).associateBy { it.id },
                    onNextRound = {}, onExit = {}
                )
            }
        }
        compose.onNodeWithText(ctx.getString(R.string.summary_score, 4, 6)).assertExists()
    }
}
