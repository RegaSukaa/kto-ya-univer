package com.kstoya.univer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kstoya.univer.R
import com.kstoya.univer.data.CharacterEntity
import com.kstoya.univer.data.PresetEntity
import com.kstoya.univer.data.Settings
import com.kstoya.univer.game.DeckFilter
import com.kstoya.univer.ui.components.BigButton
import com.kstoya.univer.ui.components.CardSurface
import com.kstoya.univer.ui.components.ChipRow
import com.kstoya.univer.ui.components.SectionTitle
import com.kstoya.univer.ui.components.ToggleRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    characters: List<CharacterEntity>,
    settings: Settings,
    presets: List<PresetEntity>,
    presetNames: (PresetEntity) -> List<String>,
    onBack: () -> Unit,
    onSavePreset: (String, List<String>) -> Unit,
    onStart: (List<String>, Int, String) -> Unit,
    onRules: () -> Unit
) {
    var count by rememberSaveable { mutableStateOf(6) }
    val names = remember { mutableStateListOf<String>().apply { repeat(10) { add("") } } }
    var showMs by rememberSaveable { mutableStateOf(settings.showMs) }
    var filter by rememberSaveable { mutableStateOf(settings.deckFilter) }
    var presetTitle by remember { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }
    val defaultPresetTitle = stringResource(R.string.setup_title)

    val filledNames = (0 until count).map { names[it].trim() }
    val filledCount = filledNames.count { it.isNotEmpty() }
    val deckChars = DeckFilter.apply(characters, filter) { it.kind }
    val canStart = filledCount >= 2 && deckChars.size >= 2
    val friendCount = deckChars.count { it.kind == "friend" }
    val teacherCount = deckChars.count { it.kind == "teacher" }
    val latest = presets.firstOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.setup_title), fontWeight = FontWeight.SemiBold) },
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
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            if (latest != null) {
                CardSurface {
                    Column {
                        Text(
                            text = stringResource(R.string.setup_preset_apply, presetNames(latest).joinToString(", ")),
                            fontSize = 14.sp
                        )
                        Row {
                            TextButton(onClick = {
                                val list = presetNames(latest)
                                count = list.size.coerceIn(2, 10)
                                list.forEachIndexed { i, n -> if (i < 10) names[i] = n }
                            }) { Text(stringResource(R.string.setup_preset_apply_btn)) }
                            TextButton(onClick = { showSaveDialog = true }) { Text(stringResource(R.string.setup_preset_save)) }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            SectionTitle(stringResource(R.string.setup_players))
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                RoundIconButton(Icons.Filled.Remove, enabled = count > 2) { count-- }
                Text("$count", fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(56.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                RoundIconButton(Icons.Filled.Add, enabled = count < 10) { count++ }
            }
            Spacer(Modifier.height(12.dp))
            for (i in 0 until count) {
                OutlinedTextField(
                    value = names[i],
                    onValueChange = { names[i] = it.take(16) },
                    label = { Text(stringResource(R.string.setup_player_n, i + 1)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Next)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { for (i in 0 until count) names[i] = "" }) { Text(stringResource(R.string.setup_clear)) }
                TextButton(onClick = { showSaveDialog = true }) { Text(stringResource(R.string.setup_preset_save)) }
            }

            Spacer(Modifier.height(24.dp))
            SectionTitle(stringResource(R.string.setup_deck))
            Spacer(Modifier.height(8.dp))
            ChipRow(
                listOf(
                    stringResource(R.string.filter_all),
                    stringResource(R.string.filter_students),
                    stringResource(R.string.filter_teachers)
                ),
                when (filter) {
                    DeckFilter.FRIENDS -> 1
                    DeckFilter.TEACHERS -> 2
                    else -> 0
                }
            ) { i ->
                filter = when (i) {
                    1 -> DeckFilter.FRIENDS
                    2 -> DeckFilter.TEACHERS
                    else -> DeckFilter.ALL
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.setup_deck, deckChars.size) + " · " +
                    stringResource(R.string.setup_deck_kinds, friendCount, teacherCount),
                fontSize = 15.sp
            )
            if (deckChars.size < count) {
                Text(
                    stringResource(R.string.setup_deck_warn),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            if (deckChars.size < 2) {
                Text(stringResource(R.string.setup_need_characters), color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            }

            Spacer(Modifier.height(24.dp))
            SectionTitle(stringResource(R.string.setup_show_time))
            Spacer(Modifier.height(8.dp))
            val times = listOf(5, 10, 15, 20, 30)
            val timeLabels = listOf(
                stringResource(R.string.setup_secs, 5),
                stringResource(R.string.setup_secs, 10),
                stringResource(R.string.setup_secs, 15),
                stringResource(R.string.setup_secs, 20),
                stringResource(R.string.setup_secs, 30)
            )
            ChipRow(timeLabels, times.indexOfFirst { it * 1000 == showMs }.coerceAtLeast(1)) { i ->
                showMs = times[i] * 1000
            }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onRules) { Text(stringResource(R.string.rules_link)) }

            Spacer(Modifier.height(24.dp))
            if (filledCount < 2) {
                Text(stringResource(R.string.setup_need_players), color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
            }
            BigButton(
                text = stringResource(R.string.setup_start),
                onClick = { onStart(filledNames, showMs, filter) },
                enabled = canStart
            )
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text(stringResource(R.string.setup_preset_save)) },
            text = {
                OutlinedTextField(
                    value = presetTitle,
                    onValueChange = { presetTitle = it.take(24) },
                    label = { Text(stringResource(R.string.setup_preset_title)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSavePreset(presetTitle.ifBlank { defaultPresetTitle }, filledNames.filter { it.isNotEmpty() })
                    showSaveDialog = false
                }) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text(stringResource(R.string.common_cancel)) } }
        )
    }
}

@Composable
private fun RoundIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(56.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
    }
}
