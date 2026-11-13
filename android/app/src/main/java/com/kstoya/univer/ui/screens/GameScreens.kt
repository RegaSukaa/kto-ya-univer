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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.kstoya.univer.ui.components.toCardModel
import com.kstoya.univer.ui.components.fallbackCard
import com.kstoya.univer.util.Haptics
import com.kstoya.univer.util.Share
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
    val card = g.card
    val round = g.rounds.getOrNull(g.index)

    val backExit: () -> Unit = { vm.closeGame(); onExit() }

    when {
        g.reshow != null -> ReshowScreen(
            vm = vm,
            round = g.reshow!!,
            card = card,
            remainingMs = g.reshowRemainingMs,
            showMs = g.showMs
        )
        g.summaryOpen && g.summary != null -> SummaryScreen(
            rounds = g.rounds,
            results = g.results,
            stats = g.summary!!,
            scored = g.markAfter,
            chars = vm.characters.collectAsState().value.associateBy { it.id },
            onNextRound = { vm.nextRound() },
            onExit = backExit
        )
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
                card = card,
                remainingMs = g.remainingMs,
                showMs = g.showMs,
                roundLabel = stringResource(R.string.pass_progress, g.index + 1, g.players.size)
            )
        }
        g.phase == LocalPhase.MARK -> {
            Immersive(onBack = backExit)
            MarkScreen(
                title = stringResource(R.string.mark_title),
                characterName = round?.characterName ?: card?.name ?: "",
                onGuessed = { vm.mark("guessed") },
                onMissed = { vm.mark("missed") },
                onSkip = { vm.skipRound() }
            )
        }
        else -> {
            ReviewScreen(
                rounds = g.rounds,
                results = g.results,
                chars = vm.characters.collectAsState().value.associateBy { it.id },
                onReshow = { vm.reshow(it) },
                onSetResult = { id, v -> vm.setReviewResult(id, v) },
                onNextRound = { vm.nextRound() },
                onOpenSummary = { vm.openSummary() },
                onExit = backExit
            )
        }
    }
}

@Composable
private fun Immersive(onBack: () -> Unit) {
    KeepBright()
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

@Composable
fun KeepBright() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = view.context.findActivity()?.window
        view.keepScreenOn = true
        val old = window?.attributes?.screenBrightness ?: -1f
        window?.let { w ->
            val a = w.attributes
            a.screenBrightness = 1f
            w.attributes = a
        }
        onDispose {
            view.keepScreenOn = false
            window?.let { w ->
                val a = w.attributes
                a.screenBrightness = old
                w.attributes = a
            }
        }
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
private fun MarkScreen(title: String, characterName: String, onGuessed: () -> Unit, onMissed: () -> Unit, onSkip: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Text(title, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text(characterName, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        BigButton(stringResource(R.string.mark_guessed), onClick = onGuessed)
        Spacer(Modifier.height(12.dp))
        BigButton(stringResource(R.string.mark_missed), onClick = onMissed, primary = false)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onSkip) { Text(stringResource(R.string.mark_skip), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ReshowScreen(vm: AppViewModel, round: RoundInfo, card: com.kstoya.univer.ui.components.CardModel?, remainingMs: Long, showMs: Int) {
    Immersive(onBack = { vm.closeReshow() })
    val seconds = ceil(remainingMs / 1000.0).toInt()
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (card != null) {
            CharacterCard(
                card = card,
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 20.dp),
                roundLabel = round.playerName
            )
        }
        IconButton(
            onClick = { vm.closeReshow() },
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
        ) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.review_close), tint = Color.White) }
        Box(Modifier.align(Alignment.TopEnd).padding(28.dp)) {
            ProgressRing(fraction = if (showMs > 0) remainingMs.toFloat() / showMs else 0f, seconds = seconds, diameter = 64.dp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReviewScreen(
    rounds: List<RoundInfo>,
    results: Map<String, String?>,
    chars: Map<String, com.kstoya.univer.data.CharacterEntity>,
    onReshow: (RoundInfo) -> Unit,
    onSetResult: (String, String) -> Unit,
    onNextRound: () -> Unit,
    onOpenSummary: () -> Unit,
    onExit: () -> Unit
) {
    var changeFor by remember { mutableStateOf<RoundInfo?>(null) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onExit, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                }
                Text(stringResource(R.string.review_title), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }
            Text(stringResource(R.string.review_subtitle), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(rounds, key = { it.playerId }) { r ->
                val ch = chars[r.characterId]
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        r.playerName,
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    CharacterCard(
                        card = ch?.toCardModel() ?: fallbackCard(r.characterName),
                        modifier = Modifier.fillMaxWidth().aspectRatio(0.75f).clip(RoundedCornerShape(14.dp))
                            .combinedClickable(
                                onClick = { onReshow(r) },
                                onLongClick = { changeFor = r }
                            ),
                        scale = 0.5f
                    )
                    Text(
                        text = statusText(results[r.playerId]),
                        fontSize = 13.sp,
                        color = statusColor(results[r.playerId]),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.weight(1f)) {
                BigButton(stringResource(R.string.review_next_round), onClick = onNextRound, primary = false)
            }
            Box(Modifier.weight(1f)) {
                BigButton(stringResource(R.string.review_finish), onClick = onOpenSummary)
            }
        }
    }

    changeFor?.let { r ->
        AlertDialog(
            onDismissRequest = { changeFor = null },
            title = { Text(r.playerName) },
            text = { Text(stringResource(R.string.review_change_result)) },
            confirmButton = {
                Column {
                    TextButton(onClick = { onSetResult(r.playerId, "guessed"); changeFor = null }) {
                        Text(stringResource(R.string.review_status_guessed))
                    }
                    TextButton(onClick = { onSetResult(r.playerId, "missed"); changeFor = null }) {
                        Text(stringResource(R.string.review_status_missed))
                    }
                    TextButton(onClick = { onSetResult(r.playerId, "skipped"); changeFor = null }) {
                        Text(stringResource(R.string.review_status_skipped))
                    }
                }
            },
            dismissButton = { TextButton(onClick = { changeFor = null }) { Text(stringResource(R.string.common_cancel)) } }
        )
    }
}

@Composable
fun statusColor(value: String?): Color = when (value) {
    "guessed" -> Color(0xFF22C55E)
    "missed" -> Color(0xFFEF4444)
    else -> Color(0xFF9AA0AA)
}

@Composable
fun statusText(value: String?): String = when (value) {
    "guessed" -> "✓ " + stringResource(R.string.review_status_guessed)
    "missed" -> "✗ " + stringResource(R.string.review_status_missed)
    "skipped" -> "— " + stringResource(R.string.review_status_skipped)
    else -> "— " + stringResource(R.string.review_status_pending)
}

@Composable
fun SummaryScreen(
    rounds: List<RoundInfo>,
    results: Map<String, String?>,
    stats: com.kstoya.univer.game.SummaryStats,
    scored: Boolean,
    chars: Map<String, com.kstoya.univer.data.CharacterEntity>,
    onNextRound: () -> Unit,
    onExit: () -> Unit
) {
    val ctx = LocalContext.current
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding()
            .verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        Text(
            stringResource(R.string.summary_title),
            fontSize = 34.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(12.dp))
        if (scored) {
            Text(stringResource(R.string.summary_score, stats.guessed, stats.total), fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            val bestText = when {
                stats.best.isEmpty() -> ""
                stats.best.size == 1 -> stringResource(R.string.summary_winner, stats.best.first())
                else -> stringResource(R.string.summary_draw, stats.best.joinToString(", "))
            }
            if (bestText.isNotEmpty()) Text(bestText, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(stringResource(R.string.summary_no_score), fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(20.dp))
        rounds.forEach { r ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val ch = chars[r.characterId]
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(10.dp))) {
                    CharacterCard(
                        card = ch?.toCardModel() ?: fallbackCard(r.characterName),
                        modifier = Modifier.fillMaxSize(),
                        scale = 0.22f,
                        photoFraction = 0.8f,
                        showType = false
                    )
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(r.playerName, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(r.characterName, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(statusText(results[r.playerId]), fontSize = 13.sp, color = statusColor(results[r.playerId]))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.summary_stats_kinds, stats.friendGuessed, stats.friendTotal, stats.teacherGuessed, stats.teacherTotal),
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            stringResource(R.string.summary_avg_show, stats.avgShowSec),
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        BigButton(stringResource(R.string.review_next_round), onClick = onNextRound)
        Spacer(Modifier.height(10.dp))
        val shareTitle = stringResource(R.string.app_name)
        val shareScore = if (scored) stringResource(R.string.share_summary_text, stats.guessed, stats.total) else ""
        val shareSubject = stringResource(R.string.share_subject)
        BigButton(
            text = stringResource(R.string.summary_share),
            primary = false,
            onClick = {
                val text = buildString {
                    appendLine(shareTitle)
                    if (shareScore.isNotEmpty()) appendLine(shareScore)
                    rounds.forEach { appendLine("${it.playerName} — ${it.characterName}") }
                }
                Share.text(ctx, text, shareSubject)
            }
        )
        Spacer(Modifier.height(10.dp))
        BigButton(stringResource(R.string.summary_menu), onClick = onExit, primary = false)
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.summary_local_only),
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
