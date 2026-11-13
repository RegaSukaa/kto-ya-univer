package com.kstoya.univer.ui.screens

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.kstoya.univer.R
import com.kstoya.univer.audio.VoicePack
import com.kstoya.univer.data.Settings
import com.kstoya.univer.ui.components.BigButton
import com.kstoya.univer.ui.components.CardSurface
import com.kstoya.univer.ui.components.SectionTitle
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoicePackScreen(voicePack: VoicePack, settings: Settings, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var recording by remember { mutableStateOf<String?>(null) }
    var tick by remember { mutableLongStateOf(0L) }
    var version by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<String?>(null) }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val slot = pending
        pending = null
        if (granted && slot != null) {
            runCatching { voicePack.start(slot) }
                .onSuccess { recording = slot }
                .onFailure { error = ctx.getString(R.string.voice_mic_denied) }
        } else if (!granted) {
            error = ctx.getString(R.string.voice_mic_denied)
        }
    }

    fun startRecording(slot: String) {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            runCatching { voicePack.start(slot) }.onSuccess { recording = slot }.onFailure { error = ctx.getString(R.string.voice_mic_denied) }
        } else {
            pending = slot
            permLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(recording) {
        val slot = recording ?: return@LaunchedEffect
        tick = 0
        while (tick < VoicePack.MAX_MS) {
            delay(100)
            tick += 100
        }
        voicePack.stop()
        recording = null
        version++
    }

    val labels = listOf(
        stringResource(R.string.voice_slot_start),
        stringResource(R.string.voice_slot_guessed),
        stringResource(R.string.voice_slot_missed),
        stringResource(R.string.voice_slot_final)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.voice_title), fontWeight = FontWeight.SemiBold) },
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
            Text(stringResource(R.string.voice_body), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.voice_mic_reason), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            SectionTitle("${voicePack.recordedCount()} / ${VoicePack.SLOTS.size}")
            Spacer(Modifier.height(8.dp))
            VoicePack.SLOTS.forEachIndexed { i, slot ->
                val has = voicePack.has(slot)
                CardSurface(modifier = Modifier.padding(vertical = 4.dp)) {
                    Column {
                        Text(labels[i], fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = when {
                                recording == slot -> stringResource(R.string.voice_recording) + " " + (tick / 1000f).toString().take(3) + "s"
                                has -> stringResource(R.string.voice_slot_recorded)
                                else -> stringResource(R.string.voice_slot_empty)
                            },
                            fontSize = 12.sp,
                            color = if (recording == slot) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        if (recording == slot) {
                            BigButton(stringResource(R.string.common_done), onClick = {
                                voicePack.stop()
                                recording = null
                                version++
                            })
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { startRecording(slot) }) {
                                    Text(stringResource(if (has) R.string.settings_rerecord else R.string.settings_record))
                                }
                                if (has) {
                                    TextButton(onClick = {
                                        val f = voicePack.fileFor(slot)
                                        val snd = (ctx.applicationContext as com.kstoya.univer.App).sounds
                                        snd.playFile(f, settings.voiceVolume)
                                    }) { Text(stringResource(R.string.settings_listen)) }
                                    TextButton(onClick = {
                                        voicePack.delete(slot)
                                        version++
                                    }) { Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error) }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.voice_max), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
