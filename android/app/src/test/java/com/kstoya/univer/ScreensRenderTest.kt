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
import com.kstoya.univer.ui.components.CharacterCard
import com.kstoya.univer.ui.components.fallbackCard
import com.kstoya.univer.ui.components.toCardModel
import com.kstoya.univer.ui.screens.HomeScreen
import com.kstoya.univer.ui.screens.ReviewScreen
import com.kstoya.univer.ui.screens.SetupScreen
import com.kstoya.univer.ui.screens.RulesScreen
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
            KtoYaTheme { HomeScreen(characters = chars(3), onLocal = {}, onOnline = {}, onLibrary = {}, onSettings = {}, onRules = {}) }
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
                    onStart = { _, _, _ -> },
                    onRules = {}
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
                    onReshow = {}, onNextRound = {}, onExit = {}
                )
            }
        }
        (0 until 6).forEach { compose.onNodeWithText("Игрок $it").assertExists() }
        compose.onNodeWithText(ctx.getString(R.string.review_show_character, "Игрок 0")).assertExists()
    }

    @Test
    fun rulesExplainTheYesNoLoop() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        compose.setContent { KtoYaTheme { RulesScreen(onBack = {}) } }
        compose.onNodeWithText(ctx.getString(R.string.rules_title)).assertExists()
        compose.onNodeWithText(ctx.getString(R.string.rules_step_3)).assertExists()
    }
}
