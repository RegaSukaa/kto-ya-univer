package com.kstoya.univer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kstoya.univer.data.CharacterEntity
import com.kstoya.univer.game.COUNTDOWN_MS
import com.kstoya.univer.game.Deck
import com.kstoya.univer.game.online.ConnStatus
import com.kstoya.univer.game.online.OnlineClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

data class OmPlayer(val id: String, val name: String, val ready: Boolean, val online: Boolean)

data class OnlineUiState(
    val status: ConnStatus = ConnStatus.IDLE,
    val serverUrl: String = "",
    val code: String = "",
    val myId: String = "",
    val myName: String = "",
    val host: Boolean = false,
    val hostId: String = "",
    val phase: String = "idle",
    val players: List<OmPlayer> = emptyList(),
    val round: Int = 1,
    val showMs: Int = 10_000,
    val startAt: Long = 0L,
    val seed: Long = 0L,
    val results: Map<String, String> = emptyMap(),
    val errorCode: String? = null,
    val skew: Long = 0L,
    val elapsedMs: Long = -1L,
    val myCharacter: CharacterEntity? = null,
    val reveal: List<Pair<String, String>> = emptyList(),
    val hidden: Boolean = false,
    val deckMismatch: Boolean = false,
    val joined: Boolean = false,
    val savedSession: Boolean = false
) {
    /** Локально вычисленная фаза: сервер присылает countdown, дальше клиент считает сам. */
    val displayPhase: String
        get() = when (phase) {
            "countdown", "show" -> when {
                elapsedMs < 0 -> "countdown"
                elapsedMs < COUNTDOWN_MS -> "countdown"
                elapsedMs < COUNTDOWN_MS + showMs -> "show"
                else -> "mark"
            }
            else -> phase
        }

    val countdownDigit: Int
        get() {
            if (elapsedMs < 0) return 3
            val e = elapsedMs
            return when {
                e < 800 -> 3
                e < 1600 -> 2
                e < 2400 -> 1
                else -> 0
            }
        }

    val showRemainingMs: Long
        get() = (COUNTDOWN_MS + showMs - elapsedMs).coerceAtLeast(0)
}

class OnlineViewModel(app: Application) : AndroidViewModel(app) {
    private val a = app as App
    private val repo = a.repo
    private val prefs = a.prefs
    private val client = OnlineClient()
    private val _state = MutableStateFlow(OnlineUiState())
    val state: StateFlow<OnlineUiState> = _state.asStateFlow()

    private var deck: List<CharacterEntity> = emptyList()
    private var pending: (() -> Unit)? = null
    private var ticker: Job? = null
    private var token: String = ""
    private var lastStartAt = 0L
    private var autoRejoinTried = false

    private val session = app.getSharedPreferences("ktya_online", 0)

    init {
        client.onMessage = { msg -> handle(msg) }
        client.onStatus = { st, err ->
            _state.update { it.copy(status = st) }
            if (st == ConnStatus.CONNECTED) hello()
            if (st == ConnStatus.FAILED || st == ConnStatus.CLOSED) {
                _state.update { it.copy(errorCode = "ROOM_CLOSED", phase = if (it.phase == "idle") "idle" else it.phase) }
            }
        }
    }

    fun attach(serverUrl: String, characters: List<CharacterEntity>) {
        deck = characters
        if (_state.value.serverUrl.isBlank()) _state.update { it.copy(serverUrl = serverUrl) }
        recomputeDeal()
        if (!autoRejoinTried) {
            autoRejoinTried = true
            val savedCode = session.getString("code", "") ?: ""
            val savedId = session.getString("playerId", "") ?: ""
            val savedToken = session.getString("token", "") ?: ""
            if (savedCode.isNotBlank() && savedId.isNotBlank() && savedToken.isNotBlank()) {
                _state.update { it.copy(savedSession = true) }
            }
        }
    }

    fun resumeSession() {
        val savedCode = session.getString("code", "") ?: return
        val savedId = session.getString("playerId", "") ?: return
        val savedToken = session.getString("token", "") ?: return
        pending = {
            client.send(JSONObject().put("type", "rejoin").put("code", savedCode).put("playerId", savedId).put("token", savedToken))
        }
        client.connect(_state.value.serverUrl)
    }

    fun dismissSaved() {
        session.edit().clear().apply()
        _state.update { it.copy(savedSession = false) }
    }

    private fun hello() {
        client.send(JSONObject().put("type", "hello").put("proto", 1).put("clientId", "android").put("appVersion", BuildConfig.VERSION_NAME).put("lang", "ru"))
        pending?.invoke()
        pending = null
    }

    fun createRoom(name: String, url: String, showHint: Boolean = false) {
        _state.update { it.copy(serverUrl = url, myName = name.trim(), errorCode = null) }
        pending = {
            val hash = repo.deckHash(deck)
            client.send(
                JSONObject().put("type", "create").put("name", name.trim())
                    .put("deckSize", deck.size).put("deckHash", hash)
            )
        }
        client.connect(url)
    }

    fun joinRoom(code: String, name: String, url: String) {
        _state.update { it.copy(serverUrl = url, myName = name.trim(), code = code.trim().uppercase(), errorCode = null) }
        pending = {
            client.send(JSONObject().put("type", "join").put("code", code.trim().uppercase()).put("name", name.trim()))
        }
        client.connect(url)
    }

    fun setReady(ready: Boolean) {
        val s = _state.value
        client.send(JSONObject().put("type", "ready").put("ready", ready))
        _state.update { st -> st.copy(players = st.players.map { if (it.id == s.myId) it.copy(ready = ready) else it }) }
    }

    fun startGame() {
        val s = _state.value
        client.send(JSONObject().put("type", "start").put("showMs", s.showMs).put("alternateKinds", true))
    }

    fun sendResult(value: String) {
        client.send(JSONObject().put("type", "result").put("round", _state.value.round).put("value", value))
    }

    fun revealAll() = client.send(JSONObject().put("type", "reveal").put("round", _state.value.round))

    fun nextRound() = client.send(JSONObject().put("type", "next_round"))

    /** Хост раскрывает карточки, когда все отметились (или сразу — по кнопке). */
    fun revealIfHost() {
        val s = _state.value
        if (s.host && s.players.isNotEmpty() && s.players.all { s.results.containsKey(it.id) }) revealAll()
    }

    fun toggleHidden() = _state.update { it.copy(hidden = !it.hidden) }

    fun leave() {
        runCatching { client.send(JSONObject().put("type", "leave")) }
        client.close()
        ticker?.cancel()
        session.edit().clear().apply()
        _state.value = OnlineUiState(savedSession = false)
    }

    override fun onCleared() {
        ticker?.cancel()
        client.close()
        super.onCleared()
    }

    private fun handle(msg: JSONObject) {
        when (msg.optString("type")) {
            "hello_ok" -> {
                val serverTime = msg.optLong("serverTime", System.currentTimeMillis())
                _state.update { it.copy(skew = serverTime - System.currentTimeMillis()) }
            }
            "joined" -> {
                val code = msg.optString("code")
                val id = msg.optString("playerId")
                token = msg.optString("token")
                session.edit().putString("code", code).putString("playerId", id).putString("token", token).apply()
                _state.update {
                    it.copy(
                        joined = true, code = code, myId = id,
                        host = msg.optString("hostId") == id, hostId = msg.optString("hostId"), savedSession = false, errorCode = null
                    )
                }
                recomputeDeal()
            }
            "room" -> {
                val players = mutableListOf<OmPlayer>()
                val arr = msg.optJSONArray("players")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val p = arr.getJSONObject(i)
                        players += OmPlayer(
                            id = p.optString("id"), name = p.optString("name"),
                            ready = p.optBoolean("ready"), online = p.optBoolean("online", true)
                        )
                    }
                }
                val results = mutableMapOf<String, String>()
                msg.optJSONObject("results")?.let { r ->
                    r.keys().forEach { k -> results[k] = r.optString(k) }
                }
                val now = System.currentTimeMillis() + _state.value.skew
                val startAt = msg.optLong("startAt", 0L)
                val phase = msg.optString("phase", "lobby")
                val myId = _state.value.myId
                _state.update {
                    it.copy(
                        code = msg.optString("code", it.code),
                        phase = phase,
                        players = players,
                        round = msg.optInt("round", it.round),
                        showMs = msg.optInt("showMs", it.showMs),
                        startAt = startAt,
                        seed = msg.optLong("seed", it.seed),
                        results = results,
                        host = msg.optString("hostId") == myId,
                        hostId = msg.optString("hostId", it.hostId),
                        deckMismatch = msg.optString("deckHash").isNotEmpty() && msg.optString("deckHash") != repo.deckHash(deck),
                        elapsedMs = if (startAt > 0) now - startAt else -1L
                    )
                }
                if (phase == "lobby") ticker?.cancel()
                if (phase == "countdown" || phase == "show") startTicker()
                recomputeDeal()
            }
            "countdown" -> {
                val startAt = msg.optLong("startAt")
                lastStartAt = startAt
                val now = System.currentTimeMillis() + _state.value.skew
                _state.update {
                    it.copy(
                        phase = "countdown",
                        startAt = startAt,
                        showMs = msg.optInt("showMs", it.showMs),
                        seed = msg.optLong("seed", it.seed),
                        round = msg.optInt("round", it.round),
                        elapsedMs = now - startAt
                    )
                }
                startTicker()
                recomputeDeal()
            }
            "error" -> {
                _state.update { it.copy(errorCode = msg.optString("code"), status = if (it.joined) it.status else ConnStatus.IDLE) }
                if (!_state.value.joined) client.close()
            }
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive) {
                val s = _state.value
                if (s.startAt <= 0) break
                val now = System.currentTimeMillis() + s.skew
                _state.update { it.copy(elapsedMs = now - it.startAt) }
                delay(100)
            }
        }
    }

    private fun recomputeDeal() {
        val s = _state.value
        if (deck.isEmpty() || s.players.isEmpty() || s.myId.isBlank()) return
        val kinds = deck.associate { it.id to it.kind }
        val ids = deck.map { it.id }
        val playerIds = s.players.map { it.id }
        if (playerIds.isEmpty()) return
        val deal = if (s.seed != 0L) Deck.deal(s.seed, ids, playerIds, true) { kinds[it] ?: "other" } else emptyMap()
        val mine = deal[s.myId]?.let { cid -> deck.firstOrNull { it.id == cid } }
        val reveal = if (s.phase == "reveal") s.players.map { p ->
            p.name to (deal[p.id]?.let { cid -> deck.firstOrNull { it.id == cid }?.name } ?: "—")
        } else emptyList()
        _state.update { it.copy(myCharacter = mine ?: it.myCharacter, reveal = reveal) }
    }
}
