package com.kstoya.univer

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kstoya.univer.data.CharacterEntity
import com.kstoya.univer.data.ImportPolicy
import com.kstoya.univer.data.PresetEntity
import com.kstoya.univer.data.Settings
import com.kstoya.univer.ui.components.toCardModel
import com.kstoya.univer.audio.SoundPriority
import com.kstoya.univer.audio.SoundSource
import com.kstoya.univer.game.COUNTDOWN_MS
import com.kstoya.univer.game.Deck
import com.kstoya.univer.game.LocalGameEngine
import com.kstoya.univer.game.LocalPhase
import com.kstoya.univer.game.PlayerSlot
import com.kstoya.univer.game.RoundInfo
import com.kstoya.univer.game.SummaryStats
import com.kstoya.univer.game.summarize
import com.kstoya.univer.ui.components.CardModel
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
    val markAfter: Boolean = false,
    val results: Map<String, String?> = emptyMap(),
    val reshow: RoundInfo? = null,
    val reshowRemainingMs: Long = 0L,
    val summary: SummaryStats? = null,
    val roundNo: Int = 1,
    val countdownMs: Long = COUNTDOWN_MS,
    val summaryOpen: Boolean = false
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val a = app as App
    private val repo = a.repo
    private val prefs = a.prefs
    private val sounds = a.sounds
    val voicePack = a.voicePack

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
    private var lastPhase: LocalPhase? = null
    private var lastDigit = -1
    private var lastSec = -1
    private var reshowRound: RoundInfo? = null
    private var reshowStart = 0L
    private var roundNo = 1
    private var isFinished = false
    private var summaryOpen = false

    init {
        viewModelScope.launch { _presets.value = repo.presets() }
    }

    // ---------- настройки ----------

    fun setShowMs(v: Int) = viewModelScope.launch { prefs.setShowMs(v) }
    fun setAlternate(v: Boolean) = viewModelScope.launch { prefs.setAlternate(v) }
    fun setMarkAfter(v: Boolean) = viewModelScope.launch { prefs.setMarkAfter(v) }
    fun setSound(v: Boolean) = viewModelScope.launch { prefs.setSound(v) }
    fun setVibrate(v: Boolean) = viewModelScope.launch { prefs.setVibrate(v) }
    fun setTapHide(v: Boolean) = viewModelScope.launch { prefs.setTapHide(v) }
    fun setVoiceVolume(v: Float) = viewModelScope.launch { prefs.setVoiceVolume(v) }
    fun setServerUrl(v: String) = viewModelScope.launch { prefs.setServerUrl(v) }
    fun setDarkTheme(v: Boolean) = viewModelScope.launch { prefs.setDarkTheme(v) }

    fun language(): String = Lang.tag(getApplication())
    fun setLanguage(tag: String) {
        Lang.set(getApplication(), tag)
        viewModelScope.launch { /* recreate делает экран */ }
    }

    fun refreshPresets() = viewModelScope.launch { _presets.value = repo.presets() }

    fun savePreset(title: String, names: List<String>) = viewModelScope.launch {
        repo.savePreset(title, names)
        _presets.value = repo.presets()
    }

    fun saveCharacter(c: CharacterEntity, onDone: () -> Unit = {}) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        val existing = repo.character(c.id)
        repo.saveCharacter(
            c.copy(
                initials = c.initials.ifBlank { com.kstoya.univer.util.initialsOf(c.name) },
                createdAt = existing?.createdAt ?: if (c.createdAt == 0L) now else c.createdAt,
                updatedAt = now
            )
        )
        onDone()
    }

    fun deleteCharacter(c: CharacterEntity, onDone: () -> Unit = {}) = viewModelScope.launch {
        repo.deleteCharacter(c)
        onDone()
    }

    fun duplicateCharacter(c: CharacterEntity) = viewModelScope.launch {
        val copy = c.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = c.name + " 2",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            photoPath = null,
            assetPhoto = c.assetPhoto,
            source = "local"
        )
        repo.saveCharacter(copy)
    }

    suspend fun tagSuggestions(): List<String> = repo.tagSuggestions()
    suspend fun allCharactersNow(): List<CharacterEntity> = repo.allCharacters()

    fun wipeData(onDone: () -> Unit = {}) = viewModelScope.launch {
        repo.wipeAll()
        repo.seedFromAssets()
        onDone()
    }

    fun refreshBank(onDone: (Int) -> Unit = {}) = viewModelScope.launch {
        val n = repo.seedFromAssets(force = true)
        onDone(n)
    }

    fun deckHash(chars: List<CharacterEntity>): String = repo.deckHash(chars)

    // ---------- локальная игра ----------

    fun startLocal(names: List<String>, showMs: Int, alternate: Boolean, markAfter: Boolean): Boolean {
        val clean = names.map { it.trim() }.filter { it.isNotEmpty() }
        val chars = characters.value
        if (clean.size < 2 || chars.size < 2) return false
        gameChars = chars
        val kinds = chars.associate { it.id to it.kind }
        val seed = System.currentTimeMillis()
        val keys = clean.indices.map { "p$it" }
        val deal = Deck.deal(seed, chars.map { it.id }, keys, alternate) { kinds[it] ?: "other" }
        val rounds = clean.mapIndexed { i, playerName ->
            val charId = deal["p$i"] ?: chars[i % chars.size].id
            val ch = chars.first { it.id == charId }
            RoundInfo(playerId = "p$i", playerName = playerName, characterId = ch.id, characterName = ch.name)
        }
        engine = LocalGameEngine(
            players = rounds.map { PlayerSlot(it.playerId, it.playerName) },
            rounds = rounds, showMs = showMs, markAfter = markAfter
        )
        engine?.start(SystemClock.elapsedRealtime())
        roundNo = 1
        isFinished = false
        summaryOpen = false
        lastPhase = null
        lastDigit = -1
        lastSec = -1
        val hash = repo.deckHash(chars)
        viewModelScope.launch {
            sessionId = repo.createSession("local", clean.size, showMs, seed, hash)
            repo.savePlayers(clean)
            rounds.forEachIndexed { i, r -> repo.addRound(sessionId, i, PlayerSlot(r.playerId, r.playerName), r.characterId, r.characterName, showMs) }
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
                    handleTransitions(before, e, now)
                    emit(now)
                }
                delay(100)
            }
        }
    }

    private fun handleTransitions(before: LocalPhase, e: LocalGameEngine, now: Long) {
        val s = settings.value
        val ctx = getApplication<Application>()
        if (e.phase != before) {
            when (e.phase) {
                LocalPhase.SHOW -> { }
                LocalPhase.MARK -> sounds.play("click", settings = s)
                LocalPhase.REVIEW -> {
                    sounds.playVoice(null, "final", s)
                    Haptics.success(ctx, s.vibrate)
                }
                else -> { }
            }
            if (before == LocalPhase.SHOW) {
                sounds.play("time_up", settings = s)
                sounds.play("ding", volume = 0.8f, settings = s)
            }
        }
        when (e.phase) {
            LocalPhase.COUNTDOWN -> {
                val d = e.countdownDigit(now)
                if (d != lastDigit) {
                    lastDigit = d
                    if (d == 0) sounds.play("go", settings = s)
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
            else -> { }
        }
        if (before != LocalPhase.REVIEW && e.phase == LocalPhase.REVIEW) {
            isFinished = true
            sessionId.takeIf { it.isNotEmpty() }?.let { id -> viewModelScope.launch { repo.finishSession(id) } }
        }
    }

    private fun emit(now: Long) {
        val e = engine ?: return
        val chars = gameChars.associateBy { it.id }
        val current = e.current
        val reshowLeft = if (reshowRound != null) (e.showMs - (now - reshowStart)).coerceAtLeast(0) else 0L
        val summary = if (e.phase == LocalPhase.REVIEW) {
            summarize(e.rounds, e.results, { id -> chars[id]?.kind ?: "other" }, e.showMs)
        } else null
        _game.value = GameUiState(
            active = true,
            phase = e.phase,
            index = e.index,
            players = e.players.map { it.name },
            rounds = e.rounds,
            card = chars[current.characterId]?.toCardModel(),
            remainingMs = e.remaining(now),
            digit = e.countdownDigit(now),
            showMs = e.showMs,
            markAfter = e.markAfter,
            results = e.results.toMap(),
            reshow = reshowRound,
            reshowRemainingMs = reshowLeft,
            summary = summary,
            roundNo = roundNo,
            summaryOpen = summaryOpen
        )
        if (reshowRound != null && reshowLeft == 0L) {
            reshowRound = null
        }
    }

    fun ready() {
        val e = engine ?: return
        val now = SystemClock.elapsedRealtime()
        e.ready(now)
        lastPhase = e.phase
        sounds.playVoice(e.current.characterId, "start", settings.value)
        Haptics.start(getApplication(), settings.value.vibrate)
        emit(now)
    }

    fun cancelRound() {
        val e = engine ?: return
        e.cancel(SystemClock.elapsedRealtime())
        lastDigit = -1
        emit(SystemClock.elapsedRealtime())
    }

    fun mark(value: String) {
        val e = engine ?: return
        val now = SystemClock.elapsedRealtime()
        val playerId = e.current.playerId
        val card = gameChars.firstOrNull { it.id == e.current.characterId }
        e.mark(now, value)
        val s = settings.value
        if (value == "guessed") {
            sounds.playVoice(card?.id, "win", s)
            Haptics.success(getApplication(), s.vibrate)
        } else {
            sounds.playVoice(card?.id, "skip", s)
            Haptics.fail(getApplication(), s.vibrate)
        }
        viewModelScope.launch { if (sessionId.isNotEmpty()) repo.setResult(sessionId, playerId, value) }
        lastDigit = -1
        lastSec = -1
        emit(now)
    }

    fun skipRound() {
        val e = engine ?: return
        val now = SystemClock.elapsedRealtime()
        val playerId = e.current.playerId
        e.skipRound(now)
        viewModelScope.launch { if (sessionId.isNotEmpty()) repo.setResult(sessionId, playerId, "skipped") }
        emit(now)
    }

    fun setReviewResult(playerId: String, value: String) {
        val e = engine ?: return
        e.setResult(playerId, value)
        viewModelScope.launch { if (sessionId.isNotEmpty()) repo.setResult(sessionId, playerId, value) }
        emit(SystemClock.elapsedRealtime())
    }

    fun reshow(round: RoundInfo) {
        reshowRound = round
        reshowStart = SystemClock.elapsedRealtime()
        emit(reshowStart)
    }

    fun closeReshow() {
        reshowRound = null
        emit(SystemClock.elapsedRealtime())
    }

    fun nextRound() {
        val names = engine?.players?.map { it.name } ?: return
        val s = settings.value
        val e = engine
        val showMs = e?.showMs ?: s.showMs
        roundNo++
        startLocal(names, showMs, s.alternateKinds, e?.markAfter ?: s.markAfter)
    }

    fun openSummary() {
        summaryOpen = true
        emit(SystemClock.elapsedRealtime())
    }

    fun endGame() {
        val e = engine ?: return
        summaryOpen = true
        e.finish(SystemClock.elapsedRealtime())
        sessionId.takeIf { it.isNotEmpty() }?.let { id -> viewModelScope.launch { repo.finishSession(id) } }
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

    fun presetNameList(p: PresetEntity): List<String> = repo.presetNames(p)

    fun soundSourceFor(characterId: String, slot: String): SoundSource =
        SoundPriority.resolve(sounds.hasPersonal(characterId, slot), voicePack.has(slot))

    fun loadPhoto(uri: android.net.Uri, onLoaded: (android.graphics.Bitmap?) -> Unit) = viewModelScope.launch {
        onLoaded(com.kstoya.univer.media.PhotoImporter.loadBitmap(getApplication(), uri))
    }

    fun processPhoto(
        uri: android.net.Uri,
        characterId: String,
        zoom: Float,
        dx: Float,
        dy: Float,
        cutout: Boolean,
        oldPath: String?,
        onResult: (String?) -> Unit
    ) = viewModelScope.launch {
        val saved = runCatching {
            com.kstoya.univer.media.PhotoImporter.process(getApplication(), uri, characterId, zoom, dx, dy, cutout)
        }.getOrNull()
        if (saved == null) { onResult(null); return@launch }
        val fresh = File(File(getApplication<Application>().filesDir, "cards"), "${characterId}_cut_${System.currentTimeMillis()}.webp")
        val ok = runCatching { File(saved.first).renameTo(fresh) }.getOrDefault(false)
        val path = if (ok) fresh.absolutePath else saved.first
        if (oldPath != null && oldPath != path) runCatching { File(oldPath).delete() }
        onResult(path)
    }

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
