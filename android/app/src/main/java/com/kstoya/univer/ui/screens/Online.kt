package com.kstoya.univer.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kstoya.univer.OnlineUiState
import com.kstoya.univer.RevealCard
import com.kstoya.univer.OnlineViewModel
import com.kstoya.univer.R
import com.kstoya.univer.data.CharacterEntity
import com.kstoya.univer.data.Settings
import com.kstoya.univer.game.COUNTDOWN_MS
import com.kstoya.univer.game.online.ConnStatus
import com.kstoya.univer.ui.components.BigButton
import com.kstoya.univer.ui.components.CharacterCard
import com.kstoya.univer.ui.components.CountdownNumber
import com.kstoya.univer.ui.components.ProgressRing
import com.kstoya.univer.ui.components.SectionTitle
import com.kstoya.univer.ui.components.toCardModel
import com.kstoya.univer.util.Share
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineScreen(
    characters: List<CharacterEntity>,
    settings: Settings,
    onBack: () -> Unit,
    onSaveServer: (String) -> Unit
) {
    val vm: OnlineViewModel = viewModel()
    val s by vm.state.collectAsState()
    val ctx = LocalContext.current
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var server by remember { mutableStateOf(settings.serverUrl) }

    LaunchedEffect(characters, settings.serverUrl) {
        vm.attach(settings.serverUrl, characters)
        if (server.isBlank()) server = settings.serverUrl
    }

    when {
        !s.joined -> OnlineEntry(
            state = s,
            name = name,
            code = code,
            server = server,
            onName = { name = it.take(16) },
            onCode = { code = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(6) },
            onServer = { server = it },
            onBack = onBack,
            onCreate = {
                if (name.isBlank()) return@OnlineEntry
                onSaveServer(server)
                vm.createRoom(name, server)
            },
            onJoin = {
                if (name.isBlank() || code.length != 6) return@OnlineEntry
                onSaveServer(server)
                vm.joinRoom(code, name, server)
            },
            onResume = { vm.resumeSession() },
            onDismissSaved = { vm.dismissSaved() }
        )
        s.phase == "closed" -> OnlineEnded(s, onBack, vm, server, name)
        s.displayPhase == "lobby" -> OnlineLobby(s, vm, onBack, settings)
        s.displayPhase == "countdown" -> {
            KeepBright()
            Box(Modifier.fillMaxSize().background(Color(0xFF2B2109))) {
                CountdownNumber(s.countdownDigit, stringResource(R.string.countdown_go), Modifier.fillMaxSize())
                Text(
                    stringResource(R.string.online_round, s.round),
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp)
                )
            }
        }
        s.displayPhase == "show" || s.displayPhase == "mark" -> OnlineGame(s, vm)
        s.phase == "reveal" -> OnlineReveal(s, vm, onBack)
        else -> OnlineLobby(s, vm, onBack, settings)
    }

    BackHandler {
        if (s.joined) vm.leave()
        onBack()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnlineEntry(
    state: OnlineUiState,
    name: String,
    code: String,
    server: String,
    onName: (String) -> Unit,
    onCode: (String) -> Unit,
    onServer: (String) -> Unit,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onJoin: () -> Unit,
    onResume: () -> Unit,
    onDismissSaved: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.online_title), fontWeight = FontWeight.SemiBold) },
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(10.dp).clip(CircleShape).background(
                        when (state.status) {
                            ConnStatus.CONNECTED -> Color(0xFF22C55E)
                            ConnStatus.CONNECTING -> Color(0xFFF59E0B)
                            ConnStatus.FAILED -> Color(0xFFEF4444)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = when (state.status) {
                        ConnStatus.CONNECTED -> stringResource(R.string.online_connected)
                        ConnStatus.CONNECTING -> stringResource(R.string.online_waking)
                        ConnStatus.FAILED -> stringResource(R.string.err_offline)
                        else -> ""
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = onName,
                label = { Text(stringResource(R.string.online_your_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            BigButton(stringResource(R.string.online_create), onClick = onCreate, enabled = name.isNotBlank())
            Spacer(Modifier.height(20.dp))
            SectionTitle(stringResource(R.string.online_join_by_code))
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = code,
                onValueChange = onCode,
                label = { Text(stringResource(R.string.online_code_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            BigButton(
                stringResource(R.string.online_join),
                onClick = onJoin,
                enabled = name.isNotBlank() && code.length == 6,
                primary = false
            )
            if (state.savedSession) {
                Spacer(Modifier.height(12.dp))
                BigButton(stringResource(R.string.online_title) + " →", onClick = onResume, primary = false)
                TextButton(onClick = onDismissSaved) { Text(stringResource(R.string.common_cancel)) }
            }
            Spacer(Modifier.height(20.dp))
            SectionTitle(stringResource(R.string.online_server))
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = server,
                onValueChange = onServer,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(onClick = { onServer(com.kstoya.univer.BuildConfig.SERVER_URL) }) {
                Text(stringResource(R.string.online_server_reset))
            }
            state.errorCode?.let {
                Spacer(Modifier.height(12.dp))
                Text(errorText(it), color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnlineLobby(state: OnlineUiState, vm: OnlineViewModel, onBack: () -> Unit, settings: Settings) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val me = state.players.firstOrNull { it.id == state.myId }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.online_lobby), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { vm.leave(); onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(20.dp)) {
            Text(
                state.code,
                fontSize = 44.sp, fontWeight = FontWeight.Bold, letterSpacing = 8.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(state.code))
                }) { Text(stringResource(R.string.online_code_copy)) }
                TextButton(onClick = {
                    Share.text(ctx, ctx.getString(R.string.online_share_code_text, state.code), ctx.getString(R.string.app_name))
                }) { Text(stringResource(R.string.online_code_share)) }
            }
            if (state.deckMismatch) {
                Text(stringResource(R.string.online_deck_mismatch), color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
            }
            Spacer(Modifier.height(12.dp))
            SectionTitle(stringResource(R.string.setup_players) + " · ${state.players.size}/10")
            Spacer(Modifier.height(8.dp))
            state.players.forEach { p ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(8.dp).clip(CircleShape)
                            .background(if (p.online) Color(0xFF22C55E) else Color(0xFF6B7280))
                    )
                    Spacer(Modifier.size(10.dp))
                    Text(p.name + if (p.id == state.myId) " (" + stringResource(R.string.online_you) + ")" else "", fontSize = 17.sp)
                    Spacer(Modifier.weight(1f))
                    if (p.id == state.hostId) {
                        Text(stringResource(R.string.online_host_badge), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    if (p.ready) Text(stringResource(R.string.online_ready), fontSize = 12.sp, color = Color(0xFF22C55E))
                }
            }
            Spacer(Modifier.weight(1f))
            if (state.host) {
                val canStart = state.players.size >= 2 && state.players.all { it.ready }
                BigButton(stringResource(R.string.online_start), onClick = { vm.startGame(settings.deckFilter) }, enabled = canStart)
                if (state.players.size < 2) {
                    Text(stringResource(R.string.online_waiting_players), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Text(stringResource(R.string.online_wait_host), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                BigButton(
                    text = stringResource(if (me?.ready == true) R.string.online_not_ready else R.string.online_ready),
                    onClick = { vm.setReady(me?.ready != true) },
                    primary = me?.ready != true
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun OnlineGame(state: OnlineUiState, vm: OnlineViewModel) {
    KeepBright()
    val card = state.myCharacter?.toCardModel(state.myPhotoIndex)
    val remaining = state.showRemainingMs
    val seconds = ceil(remaining / 1000.0).toInt()
    Box(Modifier.fillMaxSize().background(Color.Black).systemBarsPadding()) {
        if (state.hidden) {
            Column(
                Modifier.fillMaxSize().clickable { vm.toggleHidden() }.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("👁", fontSize = 64.sp)
                Text(
                    stringResource(R.string.online_hidden),
                    color = Color.White, fontSize = 20.sp, textAlign = TextAlign.Center
                )
            }
        } else if (card != null) {
            CharacterCard(
                card = card,
                modifier = Modifier.fillMaxSize().padding(20.dp).clickable { vm.toggleHidden() },
                roundLabel = stringResource(R.string.online_round, state.round)
            )
        } else {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
        }
        if (state.displayPhase == "show") {
            Box(Modifier.align(Alignment.TopEnd).padding(28.dp)) {
                ProgressRing(fraction = if (state.showMs > 0) remaining.toFloat() / (COUNTDOWN_MS + state.showMs) else 0f, seconds = seconds, diameter = 64.dp)
            }
        }
        if (state.displayPhase == "mark") {
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.85f)).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.mark_title), color = Color.White, fontSize = 20.sp)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f)) {
                        BigButton(stringResource(R.string.online_guess_yes), onClick = {
                            vm.sendResult("guessed"); vm.revealIfHost()
                        })
                    }
                    Box(Modifier.weight(1f)) {
                        BigButton(stringResource(R.string.online_guess_no), onClick = {
                            vm.sendResult("missed"); vm.revealIfHost()
                        }, primary = false)
                    }
                }
                if (state.host) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { vm.revealAll() }) {
                        Text(stringResource(R.string.online_reveal_btn), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun OnlineReveal(state: OnlineUiState, vm: OnlineViewModel, onBack: () -> Unit) {
    var openCard by remember { mutableStateOf<RevealCard?>(null) }
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding()
                .verticalScroll(rememberScrollState()).padding(20.dp)
        ) {
            Text(stringResource(R.string.online_reveal), fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.review_hidden_hint), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            state.revealCards.forEach { row ->
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(row.playerName, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    TextButton(
                        onClick = { openCard = row },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        Text(
                            stringResource(R.string.review_show_character, row.playerName),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            if (state.host) {
                BigButton(stringResource(R.string.review_next_round), onClick = { vm.nextRound() })
                Spacer(Modifier.height(8.dp))
            }
            BigButton(stringResource(R.string.summary_menu), onClick = { vm.leave(); onBack() }, primary = false)
        }
        openCard?.let { rc ->
            Box(Modifier.fillMaxSize().background(Color.Black).systemBarsPadding()) {
                val card = rc.character?.toCardModel(rc.photoIndex)
                if (card != null) {
                    CharacterCard(
                        card = card,
                        modifier = Modifier.fillMaxSize().padding(20.dp).clickable { openCard = null },
                        roundLabel = rc.playerName
                    )
                }
                IconButton(
                    onClick = { openCard = null },
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.review_close), tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun OnlineEnded(state: OnlineUiState, onBack: () -> Unit, vm: OnlineViewModel, server: String, name: String) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.online_ended), fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.online_ended_body), fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        BigButton(stringResource(R.string.online_new_room), onClick = { vm.leave(); vm.createRoom(name, server) })
        Spacer(Modifier.height(8.dp))
        BigButton(stringResource(R.string.summary_menu), onClick = { vm.leave(); onBack() }, primary = false)
    }
}

@Composable
private fun errorText(code: String): String = when (code) {
    "ROOM_NOT_FOUND" -> stringResource(R.string.err_room_not_found)
    "ROOM_FULL" -> stringResource(R.string.err_room_full)
    "NAME_TAKEN" -> stringResource(R.string.err_name_taken)
    "ALREADY_STARTED" -> stringResource(R.string.err_already_started)
    "NOT_HOST" -> stringResource(R.string.err_not_host)
    "BAD_VERSION" -> stringResource(R.string.err_bad_version)
    "ROOM_CLOSED" -> stringResource(R.string.err_offline)
    else -> stringResource(R.string.err_generic)
}
