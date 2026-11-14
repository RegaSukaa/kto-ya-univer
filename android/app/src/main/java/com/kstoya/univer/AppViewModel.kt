package com.kstoya.univer

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kstoya.univer.data.CharacterEntity
import com.kstoya.univer.data.ImportPolicy
import com.kstoya.univer.data.PresetEntity
import com.kstoya.univer.data.Settings
import com.kstoya.univer.data.assetPhotoPaths
import com.kstoya.univer.game.COUNTDOWN_MS
import com.kstoya.univer.game.Deck
import com.kstoya.univer.game.DeckFilter
import com.kstoya.univer.game.LocalGameEngine
import com.kstoya.univer.game.LocalPhase
import com.kstoya.univer.game.PlayerSlot
import com.kstoya.univer.game.RoundInfo
import com.kstoya.univer.game.digitFor
import com.kstoya.univer.ui.components.CardModel
import com.kstoya.univer.ui.components.toCardModel
import com.kstoya.univer.util.Haptics
import com.kstoya.univer.util.Lang
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.ceil

data class GameUiState(
    val active: Boolean = false,
    val phase: LocalPhase = LocalPhase.PASS,
    val index: Int = 0,
    val players: List<String> = emptyList(),
    val rounds: List<RoundInfo> = emptyList(),
    val card: CardModel? = null,
    val remainingMs: Long = 0L,
    val digit: Int = 3,
    val showMs: Int = 10_000,
    /** повторный показ из разбора: null | "countdown" | "show" */
    val reshowStage: String? = null,
    val reshowCard: CardModel? = null,
    val reshowPlayer: String = "",
    val reshowDigit: Int = 3,
    val reshowRemainingMs: Long = 0L,
    val roundNo: Int = 1,
    val countdownMs: Long = COUNTDOWN_MS
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val a = app as App
    private val repo = a.repo
    private val prefs = a.prefs
    private val sounds = a.sounds

    val characters: StateFlow<List<CharacterEntity>> =
        repo.chars.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val settings: StateFlow<Settings> =
        prefs.settings.stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    private val _presets = MutableStateFlow<List<PresetEntity>>(emptyList())
    val presets: StateFlow<List<PresetEntity>> = _presets.asStateFlow()

    private val _game = MutableStateFlow(GameUiState())
    val game: StateFlow<GameUiState> = _game.asStateFlow()

    private var engine: LocalGameEngine? = null
    private var gameChars: List<CharacterEntity> = emptyList()
    private var sessionId: String = ""
    private var ticker: Job? = null
    private var lastDigit = -1
    private var lastSec = -1
    private var reshowRound: RoundInfo? = null
    private var reshowStart = 0L
    private var lastReshowDigit = -1
    private var lastReshowSec = -1
    private var roundNo = 1

    init {
        viewModelScope.launch { _presets.value = repo.presets() }
    }

    // ---------- настройки ----------

    fun setShowMs(v: Int) = viewModelScope.launch { prefs.setShowMs(v) }
    fun setSound(v: Boolean) = viewModelScope.launch { prefs.setSound(v) }
    fun setVibrate(v: Boolean) = viewModelScope.launch { prefs.setVibrate(v) }
    fun setTapHide(v: Boolean) = viewModelScope.launch { prefs.setTapHide(v) }
    fun setVoiceVolume(v: Float) = viewModelScope.launch { prefs.setVoiceVolume(v) }
    fun setServerUrl(v: String) = viewModelScope.launch { prefs.setServerUrl(v) }
    fun setDarkTheme(v: Boolean) = viewModelScope.launch { prefs.setDarkTheme(v) }
    fun setDeckFilter(v: String) = viewModelScope.launch { prefs.setDeckFilter(v) }

    fun language(): String = Lang.tag(getApplication())
    fun setLanguage(tag: String) = Lang.set(getApplication(), tag)

    fun refreshPresets() = viewModelScope.launch { _presets.value = repo.presets() }

    fun savePreset(title: String, names: List<String>) = viewModelScope.launch {
        repo.savePreset(title, names)
        _presets.value = repo.presets()
    }

    fun presetNameList(p: PresetEntity): List<String> = repo.presetNames(p)

    suspend fun tagSuggestions(): List<String> = repo.tagSuggestions()
    fun deckHash(chars: List<CharacterEntity>): String = repo.deckHash(chars)

    fun wipeData(onDone: () -> Unit = {}) = viewModelScope.launch {
        repo.wipeAll()
        repo.seedFromAssets()
        onDone()
    }

    fun refreshBank(onDone: (Int) -> Unit = {}) = viewModelScope.launch {
        val n = repo.seedFromAssets(force = true)
        onDone(n)
    }

    // ---------- локальная игра ----------

    fun startLocal(names: List<String>, showMs: Int, filter: String = DeckFilter.ALL): Boolean {
        val clean = names.map { it.trim() }.filter { it.isNotEmpty() }
        val allChars = characters.value
        val chars = DeckFilter.apply(allChars, filter) { it.kind }
        if (clean.size < 2 || chars.size < 2) return false
        gameChars = chars
        val seed = System.currentTimeMillis()
        val keys = clean.indices.map { "p$it" }
        // Раздача полностью случайная, без чередования типов.
        val deal = Deck.deal(seed, chars.map { it.id }, keys, alternateKinds = false) { "other" }
        val rounds = clean.mapIndexed { i, playerName ->
            val charId = deal["p$i"] ?: chars[i % chars.size].id
            val ch = chars.first { it.id == charId }
            val photos = maxOf(1, if (ch.photoPath != null) 1 else ch.assetPhotoPaths().size)
            RoundInfo(
                playerId = "p$i",
                playerName = playerName,
                characterId = ch.id,
                characterName = ch.name,
                photoIndex = java.util.Random(seed + i).nextInt(photos)
            )
        }
        engine = LocalGameEngine(
            players = rounds.map { PlayerSlot(it.playerId, it.playerName) },
            rounds = rounds,
            showMs = showMs
        )
        engine?.start(SystemClock.elapsedRealtime())
        roundNo = 1
        lastDigit = -1
        lastSec = -1
        reshowRound = null
        val hash = repo.deckHash(chars)
        viewModelScope.launch {
            sessionId = repo.createSession("local", clean.size, showMs, seed, hash)
            repo.savePlayers(clean)
            rounds.forEachIndexed { i, r ->
                repo.addRound(sessionId, i, PlayerSlot(r.playerId, r.playerName), r.characterId, r.characterName, showMs)
            }
        }
        emit(SystemClock.elapsedRealtime())
        startTicker()
        return true
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive) {
                val e = engine ?: break
                if (_game.value.active) {
                    val now = SystemClock.elapsedRealtime()
                    val before = e.phase
                    e.tick(now)
                    handleGameTransitions(before, e)
                    handleReshow(now)
                    emit(now)
                }
                delay(100)
            }
        }
    }

    private fun handleGameTransitions(before: LocalPhase, e: LocalGameEngine) {
        val s = settings.value
        val ctx = getApplication<Application>()
        val now = SystemClock.elapsedRealtime()
        if (e.phase != before) {
            when (e.phase) {
                LocalPhase.REVIEW -> {
                    sounds.playVoice("final", s)
                    Haptics.success(ctx, s.vibrate)
                    sessionId.takeIf { it.isNotEmpty() }?.let { id -> viewModelScope.launch { repo.finishSession(id) } }
                }
                LocalPhase.SHOW -> Unit
                else -> Unit
            }
            if (before == LocalPhase.SHOW) {
                sounds.play("time_up", settings = s)
                sounds.play("ding", volume = 0.8f, settings = s)
            }
        }
        val goVoiceAudible = sounds.hasBank("go") && s.voiceVolume > 0.05f
        when (e.phase) {
            LocalPhase.COUNTDOWN -> {
                val d = e.countdownDigit(now)
                if (d != lastDigit) {
                    lastDigit = d
                    if (!goVoiceAudible) {
                        if (d in 1..3) sounds.play("tick", 0.8f, s)
                        if (d == 0) sounds.play("go", settings = s)
                    }
                }
            }
            LocalPhase.SHOW -> {
                val sec = ceil(e.remaining(now) / 1000.0).toInt()
                if (sec != lastSec) {
                    lastSec = sec
                    when (sec) {
                        3 -> sounds.play("tick", 0.5f, s)
                        2 -> sounds.play("tick2", 0.7f, s)
                        1 -> sounds.play("tick3", 1f, s)
                    }
                    if (sec in 1..3) Haptics.tick(ctx, s.vibrate)
                }
            }
            else -> Unit
        }
    }

    /** Повторный показ: сначала 3-2-1, потом карточка на showMs. */
    private fun handleReshow(now: Long) {
        val round = reshowRound ?: return
        val e = engine ?: return
        val s = settings.value
        val ctx = getApplication<Application>()
        val elapsed = now - reshowStart
        if (elapsed < COUNTDOWN_MS) {
            val d = digitFor(elapsed)
            if (d != lastReshowDigit) {
                lastReshowDigit = d
                if (d in 1..3) {
                    if (!(sounds.hasBank("go") && s.voiceVolume > 0.05f)) sounds.play("tick", 0.8f, s)
                    Haptics.tick(ctx, s.vibrate)
                }
                if (d == 0 && !(sounds.hasBank("go") && s.voiceVolume > 0.05f)) sounds.play("go", settings = s)
            }
        } else {
            val left = e.showMs - (elapsed - COUNTDOWN_MS)
            val sec = ceil(left / 1000.0).toInt()
            if (sec != lastReshowSec) {
                lastReshowSec = sec
                when (sec) {
                    3 -> sounds.play("tick", 0.5f, s)
                    2 -> sounds.play("tick2", 0.7f, s)
                    1 -> sounds.play("tick3", 1f, s)
                }
                if (sec in 1..3) Haptics.tick(ctx, s.vibrate)
            }
            if (left <= 0) {
                sounds.play("time_up", settings = s)
                sounds.play("ding", volume = 0.8f, settings = s)
                reshowRound = null
                lastReshowDigit = -1
                lastReshowSec = -1
            }
        }
    }

    private fun emit(now: Long) {
        val e = engine ?: return
        val chars = gameChars.associateBy { it.id }
        val current = e.current
        val elapsed = if (reshowRound != null) now - reshowStart else 0L
        val reshowStage = when {
            reshowRound == null -> null
            elapsed < COUNTDOWN_MS -> "countdown"
            else -> "show"
        }
        val reshowLeft = if (reshowStage == "show") (e.showMs - (elapsed - COUNTDOWN_MS)).coerceAtLeast(0) else 0L
        _game.value = GameUiState(
            active = true,
            phase = e.phase,
            index = e.index,
            players = e.players.map { it.name },
            rounds = e.rounds,
            card = chars[current.characterId]?.toCardModel(current.photoIndex),
            remainingMs = e.remaining(now),
            digit = e.countdownDigit(now),
            showMs = e.showMs,
            reshowStage = reshowStage,
            reshowCard = reshowRound?.let { r ->
                chars[r.characterId]?.toCardModel(r.photoIndex)
            },
            reshowPlayer = reshowRound?.playerName.orEmpty(),
            reshowDigit = digitFor(elapsed),
            reshowRemainingMs = reshowLeft,
            roundNo = roundNo
        )
    }

    fun ready() {
        val e = engine ?: return
        val now = SystemClock.elapsedRealtime()
        e.ready(now)
        sounds.playVoice("go", settings.value)
        Haptics.start(getApplication(), settings.value.vibrate)
        emit(now)
    }

    fun cancelRound() {
        val e = engine ?: return
        e.cancel(SystemClock.elapsedRealtime())
        lastDigit = -1
        emit(SystemClock.elapsedRealtime())
    }

    fun reshow(round: RoundInfo) {
        reshowRound = round
        reshowStart = SystemClock.elapsedRealtime()
        lastReshowDigit = -1
        lastReshowSec = -1
        sounds.playVoice("go", settings.value)
        emit(reshowStart)
    }

    fun closeReshow() {
        reshowRound = null
        lastReshowDigit = -1
        lastReshowSec = -1
        emit(SystemClock.elapsedRealtime())
    }

    fun nextRound() {
        val names = engine?.players?.map { it.name } ?: return
        val showMs = engine?.showMs ?: settings.value.showMs
        roundNo++
        startLocal(names, showMs, settings.value.deckFilter)
    }

    fun endGame() {
        val e = engine ?: return
        e.finish(SystemClock.elapsedRealtime())
        sessionId.takeIf { it.isNotEmpty() }?.let { id -> viewModelScope.launch { repo.finishSession(id) } }
        sounds.playVoice("final", settings.value)
        emit(SystemClock.elapsedRealtime())
    }

    fun closeGame() {
        ticker?.cancel()
        ticker = null
        engine = null
        reshowRound = null
        _game.value = GameUiState()
    }

    fun playClick() = sounds.play("click", settings = settings.value)

    fun exportFile(chars: List<CharacterEntity>, title: String): File? {
        if (chars.isEmpty()) return null
        val dir = File(getApplication<Application>().cacheDir, "sets").apply { mkdirs() }
        val f = File(dir, "univer_${System.currentTimeMillis()}.ktya")
        return runCatching { repo.exportToFile(chars, title, f); f }.getOrNull()
    }

    fun importPreview(uri: android.net.Uri, onResult: (com.kstoya.univer.data.ImportPreview?) -> Unit) =
        viewModelScope.launch {
            onResult(runCatching { repo.previewImport(uri) }.getOrNull())
        }

    fun applyImport(preview: com.kstoya.univer.data.ImportPreview, policy: ImportPolicy, onDone: (Int) -> Unit) =
        viewModelScope.launch { onDone(repo.applyImport(preview, policy)) }
}
