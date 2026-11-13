package com.kstoya.univer.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kstoya.univer.BuildConfig
import com.kstoya.univer.R
import com.kstoya.univer.data.ImportPolicy
import com.kstoya.univer.data.ImportPreview
import com.kstoya.univer.data.Settings
import com.kstoya.univer.game.DeckFilter
import com.kstoya.univer.ui.components.BigButton
import com.kstoya.univer.ui.components.CardSurface
import com.kstoya.univer.ui.components.ChipRow
import com.kstoya.univer.ui.components.SectionTitle
import com.kstoya.univer.ui.components.ToggleRow
import com.kstoya.univer.util.Lang
import com.kstoya.univer.util.Share
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    language: String,
    onBack: () -> Unit,
    onShowMs: (Int) -> Unit,
    onAlternate: (Boolean) -> Unit,
    onMarkAfter: (Boolean) -> Unit,
    onSound: (Boolean) -> Unit,
    onVibrate: (Boolean) -> Unit,
    onTapHide: (Boolean) -> Unit,
    onVoiceVolume: (Float) -> Unit,
    onDarkTheme: (Boolean) -> Unit,
    onLanguage: (String) -> Unit,
    onDeckFilter: (String) -> Unit,
    onPlaceholders: () -> Unit,
    onAbout: () -> Unit,
    onWipe: ((() -> Unit) -> Unit),
    onExport: (File) -> Unit,
    onExportSet: () -> File?,
    onImport: (android.net.Uri, (ImportPreview?) -> Unit) -> Unit,
    onApplyImport: (ImportPreview, ImportPolicy, (Int) -> Unit) -> Unit
) {
    val ctx = LocalContext.current
    var wipeDialog by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var wipeStep by remember { mutableStateOf(0) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onImport(uri) { p ->
            if (p == null) message = ctx.getString(R.string.import_failed)
            else onApplyImport(p, ImportPolicy.REPLACE) { n -> message = ctx.getString(R.string.import_done, n) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {

            SectionTitle(stringResource(R.string.settings_game))
            Spacer(Modifier.height(8.dp))
            CardSurface {
                Column {
                    Text(stringResource(R.string.settings_show_time), fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    val times = listOf(5, 10, 15, 20, 30)
                    val timeLabels = listOf(
                        stringResource(R.string.setup_secs, 5),
                        stringResource(R.string.setup_secs, 10),
                        stringResource(R.string.setup_secs, 15),
                        stringResource(R.string.setup_secs, 20),
                        stringResource(R.string.setup_secs, 30)
                    )
                    ChipRow(
                        timeLabels,
                        times.indexOfFirst { it * 1000 == settings.showMs }.coerceAtLeast(1)
                    ) { i -> onShowMs(times[i] * 1000) }
                    Spacer(Modifier.height(8.dp))
                    ToggleRow(stringResource(R.string.settings_alternate), settings.alternateKinds, onAlternate)
                    ToggleRow(stringResource(R.string.settings_mark_after), settings.markAfter, onMarkAfter)
                    ToggleRow(stringResource(R.string.settings_tap_hide), settings.tapHide, onTapHide)
                    ToggleRow(stringResource(R.string.app_name) + " — " + stringResource(R.string.settings_theme), settings.darkTheme, onDarkTheme)
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.setup_deck_choose), fontSize = 16.sp)
                    Spacer(Modifier.height(6.dp))
                    ChipRow(
                        listOf(
                            stringResource(R.string.filter_all),
                            stringResource(R.string.filter_students),
                            stringResource(R.string.filter_teachers)
                        ),
                        when (settings.deckFilter) {
                            DeckFilter.FRIENDS -> 1
                            DeckFilter.TEACHERS -> 2
                            else -> 0
                        }
                    ) { i ->
                        onDeckFilter(
                            when (i) {
                                1 -> DeckFilter.FRIENDS
                                2 -> DeckFilter.TEACHERS
                                else -> DeckFilter.ALL
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionTitle(stringResource(R.string.settings_sound_block))
            Spacer(Modifier.height(8.dp))
            CardSurface {
                Column {
                    ToggleRow(stringResource(R.string.settings_sound), settings.sound, onSound)
                    ToggleRow(stringResource(R.string.settings_vibrate), settings.vibrate, onVibrate)
                    Text(stringResource(R.string.settings_voice_volume), fontSize = 16.sp)
                    Slider(value = settings.voiceVolume, onValueChange = onVoiceVolume, valueRange = 0f..1f)
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionTitle(stringResource(R.string.settings_data))
            Spacer(Modifier.height(8.dp))
            CardSurface {
                Column {
                    TextButton(onClick = {
                        val f = onExportSet()
                        if (f == null) message = ctx.getString(R.string.export_empty) else onExport(f)
                    }) { Text(stringResource(R.string.settings_export)) }
                    TextButton(onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) }) {
                        Text(stringResource(R.string.settings_import))
                    }
                    TextButton(onClick = onPlaceholders) { Text(stringResource(R.string.settings_placeholders)) }
                    TextButton(onClick = { wipeStep = 0; wipeDialog = true }) {
                        Text(stringResource(R.string.settings_wipe), color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionTitle(stringResource(R.string.settings_language))
            Spacer(Modifier.height(8.dp))
            ChipRow(listOf(stringResource(R.string.lang_ru), stringResource(R.string.lang_en)), if (language == Lang.EN) 1 else 0) { i ->
                onLanguage(if (i == 1) Lang.EN else Lang.RU)
            }

            Spacer(Modifier.height(20.dp))
            SectionTitle(stringResource(R.string.settings_about))
            Spacer(Modifier.height(8.dp))
            CardSurface {
                Column {
                    TextButton(onClick = onAbout) { Text(stringResource(R.string.settings_about)) }
                    Text(
                        stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        stringResource(R.string.about_offline),
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (wipeDialog) {
        AlertDialog(
            onDismissRequest = { wipeDialog = false },
            title = { Text(stringResource(if (wipeStep == 0) R.string.settings_wipe else R.string.settings_wipe_confirm)) },
            text = { Text(stringResource(R.string.settings_wipe_body)) },
            confirmButton = {
                TextButton(onClick = {
                    if (wipeStep == 0) wipeStep = 1
                    else {
                        wipeDialog = false
                        onWipe {
                            message = ctx.getString(R.string.settings_wipe_done)
                        }
                    }
                }) { Text(stringResource(R.string.settings_wipe_yes), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { wipeDialog = false }) { Text(stringResource(R.string.common_cancel)) } }
        )
    }

    message?.let {
        AlertDialog(
            onDismissRequest = { message = null },
            text = { Text(it) },
            confirmButton = { TextButton(onClick = { message = null }) { Text(stringResource(R.string.common_ok)) } }
        )
    }
}
