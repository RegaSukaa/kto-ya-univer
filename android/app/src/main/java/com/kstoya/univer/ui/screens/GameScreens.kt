package com.kstoya.univer.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kstoya.univer.AppViewModel
import com.kstoya.univer.R
import com.kstoya.univer.game.LocalPhase
import com.kstoya.univer.game.RoundInfo
import com.kstoya.univer.ui.components.BigButton
import com.kstoya.univer.ui.components.CharacterCard
import com.kstoya.univer.ui.components.CountdownNumber
import com.kstoya.univer.ui.components.ProgressRing
import com.kstoya.univer.util.Haptics
import com.kstoya.univer.util.findActivity
import kotlin.math.ceil

@Composable
fun GameHost(vm: AppViewModel, onExit: () -> Unit) {
    val g by vm.game.collectAsState()
    val settings by vm.settings.collectAsState()
    val ctx = LocalContext.current
    if (!g.active) {
        LaunchedEffect(Unit) { onExit() }
        return
    }

    val backExit: () -> Unit = { vm.closeGame(); onExit() }

    when {
        g.reshowStage == "countdown" -> {
            Immersive(onBack = { vm.closeReshow() })
            ReshowCountdown(player = g.reshowPlayer, digit = g.reshowDigit)
        }
        g.reshowStage == "show" -> {
            Immersive(onBack = { vm.closeReshow() })
            ReshowCard(
                card = g.reshowCard,
                player = g.reshowPlayer,
                remainingMs = g.reshowRemainingMs,
                showMs = g.showMs,
                onClose = { vm.closeReshow() }
            )
        }
        g.phase == LocalPhase.PASS -> {
            Immersive(onBack = backExit)
            val name = g.players.getOrNull(g.index) ?: ""
            PassScreen(
                name = name,
                index = g.index + 1,
                total = g.players.size,
                onReady = { Haptics.tick(ctx, settings.vibrate); vm.ready() },
                onExit = backExit,
                onFinish = { vm.endGame() }
            )
        }
        g.phase == LocalPhase.COUNTDOWN -> {
            Immersive(onBack = { vm.cancelRound() })
            var showCancel by remember { mutableStateOf(false) }
            BackHandler { showCancel = true }
            CountdownScreen(digit = g.digit, label = stringResource(R.string.countdown_go))
            if (showCancel) {
                AlertDialog(
                    onDismissRequest = { showCancel = false },
                    title = { Text(stringResource(R.string.countdown_cancel)) },
                    confirmButton = {
                        TextButton(onClick = { showCancel = false; vm.cancelRound() }) {
                            Text(stringResource(R.string.countdown_cancel_yes))
                        }
                    },
                    dismissButton = { TextButton(onClick = { showCancel = false }) { Text(stringResource(R.string.countdown_cancel_no)) } }
                )
            }
        }
        g.phase == LocalPhase.SHOW -> {
            Immersive(onBack = backExit)
            ShowScreen(
                card = g.card,
                remainingMs = g.remainingMs,
                showMs = g.showMs,
                roundLabel = stringResource(R.string.pass_progress, g.index + 1, g.players.size)
            )
        }
        else -> {
            ReviewScreen(
                rounds = g.rounds,
                onReshow = { vm.reshow(it) },
                onNextRound = { vm.nextRound() },
                onExit = backExit
            )
        }
    }
}

@Composable
private fun Immersive(onBack: () -> Unit) {
    KeepScreenOn()
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }
    BackHandler(onBack = onBack)
}

/** Только не даём экрану погаснуть. Яркость не трогаем. */
@Composable
fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
private fun PassScreen(name: String, index: Int, total: Int, onReady: () -> Unit, onExit: () -> Unit, onFinish: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF1A1A21), Color(0xFF101014)))
        )
    ) {
        IconButton(onClick = onExit, modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = Color.White.copy(alpha = 0.7f))
        }
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.4f))
            Text(
                text = stringResource(R.string.pass_progress, index, total),
                color = Color.White.copy(alpha = 0.6f), fontSize = 16.sp
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.pass_title, name),
                fontSize = 40.sp, lineHeight = 46.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, color = Color.White
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.pass_body),
                fontSize = 20.sp, lineHeight = 28.sp, textAlign = TextAlign.Center,
                color = Color.White.copy(alpha = 0.75f)
            )
            Spacer(Modifier.weight(1f))
            BigButton(stringResource(R.string.pass_ready), onClick = onReady)
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onFinish) {
                Text(stringResource(R.string.review_finish), color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CountdownScreen(digit: Int, label: String) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF2B2109), Color(0xFF101014))))) {
        CountdownNumber(digit = digit, label = label, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun ShowScreen(
    card: com.kstoya.univer.ui.components.CardModel?,
    remainingMs: Long,
    showMs: Int,
    roundLabel: String
) {
    val seconds = ceil(remainingMs / 1000.0).toInt()
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (card != null) {
            CharacterCard(
                card = card,
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 20.dp),
                roundLabel = roundLabel
            )
        }
        Box(Modifier.align(Alignment.TopEnd).padding(28.dp)) {
            ProgressRing(
                fraction = if (showMs > 0) remainingMs.toFloat() / showMs else 0f,
                seconds = seconds,
                diameter = 64.dp
            )
        }
    }
}

@Composable
private fun ReshowCountdown(player: String, digit: Int) {
    Column(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF2B2109), Color(0xFF101014)))),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(56.dp))
        Text(
            text = stringResource(R.string.reshow_for, player),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
        CountdownNumber(digit = digit, label = stringResource(R.string.countdown_go), modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun ReshowCard(
    card: com.kstoya.univer.ui.components.CardModel?,
    player: String,
    remainingMs: Long,
    showMs: Int,
    onClose: () -> Unit
) {
    val seconds = ceil(remainingMs / 1000.0).toInt()
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (card != null) {
            CharacterCard(
                card = card,
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 20.dp),
                roundLabel = player
            )
        }
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.review_close), tint = Color.White)
        }
        Box(Modifier.align(Alignment.TopEnd).padding(28.dp)) {
            ProgressRing(
                fraction = if (showMs > 0) remainingMs.toFloat() / showMs else 0f,
                seconds = seconds,
                diameter = 64.dp
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReviewScreen(
    rounds: List<RoundInfo>,
    onReshow: (RoundInfo) -> Unit,
    onNextRound: () -> Unit,
    onExit: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onExit, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                }
                Text(stringResource(R.string.review_title), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }
            Text(stringResource(R.string.review_hidden_hint), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(rounds, key = { it.playerId }) { r ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        r.playerName,
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Box(
                        modifier = Modifier.fillMaxWidth().aspectRatio(0.75f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF26262F), Color(0xFF15151B)))
                            )
                            .combinedClickable(
                                onClick = { onReshow(r) },
                                onLongClick = { onReshow(r) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text("?", fontSize = 40.sp, color = Color.White.copy(alpha = 0.35f), fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.review_show_character, r.playerName),
                                fontSize = 13.sp,
                                lineHeight = 17.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                                maxLines = 3
                            )
                        }
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) {
                    BigButton(stringResource(R.string.review_next_round), onClick = onNextRound, primary = false)
                }
                Box(Modifier.weight(1f)) {
                    BigButton(stringResource(R.string.summary_menu), onClick = onExit, primary = false)
                }
            }
        }
    }
}
