package com.kstoya.univer.game

enum class LocalPhase { PASS, COUNTDOWN, SHOW, MARK, REVIEW }

data class PlayerSlot(val id: String, val name: String)

data class RoundInfo(
    val playerId: String,
    val playerName: String,
    val characterId: String,
    val characterName: String,
    /** какое из фото персонажа показываем в этом раунде */
    val photoIndex: Int = 0
)

const val COUNT_STEP_MS = 800L
const val GO_MS = 400L
const val COUNTDOWN_MS = 3 * COUNT_STEP_MS + GO_MS // 2800

/**
 * Конечный автомат одного телефона. Время передаётся снаружи (SystemClock.elapsedRealtime),
 * поэтому вся механика проверяется юнит-тестом на виртуальном времени.
 */
class LocalGameEngine(
    val players: List<PlayerSlot>,
    val rounds: List<RoundInfo>,
    val showMs: Int,
    val markAfter: Boolean
) {
    var phase: LocalPhase = LocalPhase.PASS
        private set
    var index: Int = 0
        private set
    var phaseStart: Long = 0L
        private set
    var finished: Boolean = false
        private set
    val results: MutableMap<String, String?> = LinkedHashMap()

    val current: RoundInfo get() = rounds[index.coerceIn(0, (rounds.size - 1).coerceAtLeast(0))]

    fun start(now: Long) {
        phase = LocalPhase.PASS
        index = 0
        phaseStart = now
        finished = false
        results.clear()
        rounds.forEach { results[it.playerId] = null }
    }

    fun ready(now: Long) {
        if (phase == LocalPhase.PASS) {
            phase = LocalPhase.COUNTDOWN
            phaseStart = now
        }
    }

    fun tick(now: Long) {
        var guard = 0
        while (guard++ < 16) {
            when (phase) {
                LocalPhase.COUNTDOWN -> {
                    val end = phaseStart + COUNTDOWN_MS
                    if (now >= end) {
                        phase = LocalPhase.SHOW
                        phaseStart = end
                    } else return
                }
                LocalPhase.SHOW -> {
                    val end = phaseStart + showMs
                    if (now >= end) {
                        if (markAfter) {
                            phase = LocalPhase.MARK
                            phaseStart = end
                        } else {
                            advance(end)
                        }
                    } else return
                }
                else -> return
            }
        }
    }

    fun remaining(now: Long): Long = when (phase) {
        LocalPhase.COUNTDOWN -> (COUNTDOWN_MS - (now - phaseStart)).coerceIn(0, COUNTDOWN_MS)
        LocalPhase.SHOW -> (showMs - (now - phaseStart)).coerceIn(0, showMs.toLong())
        else -> 0L
    }

    /** 3, 2, 1, затем 0 = «Погнали!» */
    fun countdownDigit(now: Long): Int {
        val elapsed = now - phaseStart
        return when {
            elapsed < COUNT_STEP_MS -> 3
            elapsed < 2 * COUNT_STEP_MS -> 2
            elapsed < 3 * COUNT_STEP_MS -> 1
            else -> 0
        }
    }

    fun mark(now: Long, value: String) {
        if (phase != LocalPhase.MARK) return
        results[current.playerId] = value
        advance(now)
    }

    fun skipRound(now: Long) {
        results[current.playerId] = "skipped"
        advance(now)
    }

    fun cancel(now: Long) {
        if (phase == LocalPhase.COUNTDOWN || phase == LocalPhase.SHOW) {
            phase = LocalPhase.PASS
            phaseStart = now
        }
    }

    fun advance(now: Long) {
        if (index + 1 >= rounds.size) {
            finished = true
            phase = LocalPhase.REVIEW
        } else {
            index++
            phase = LocalPhase.PASS
        }
        phaseStart = now
    }

    fun setResult(playerId: String, value: String?) {
        results[playerId] = value
    }

    fun finish(now: Long) {
        finished = true
        phase = LocalPhase.REVIEW
        phaseStart = now
    }
}

data class SummaryStats(
    val guessed: Int,
    val total: Int,
    val best: List<String>,
    val friendGuessed: Int,
    val friendTotal: Int,
    val teacherGuessed: Int,
    val teacherTotal: Int,
    val avgShowSec: Int
)

fun summarize(
    rounds: List<RoundInfo>,
    results: Map<String, String?>,
    kindOf: (String) -> String,
    showMs: Int
): SummaryStats {
    val total = rounds.size
    val guessed = rounds.count { results[it.playerId] == "guessed" }
    val scores = rounds.groupBy { it.playerId }.map { (id, rs) ->
        id to rs.count { results[it.playerId] == "guessed" }
    }
    val max = scores.maxOfOrNull { it.second } ?: 0
    val best = if (max <= 0) emptyList() else scores.filter { it.second == max }.map { s ->
        rounds.firstOrNull { it.playerId == s.first }?.playerName ?: ""
    }
    val fTotal = rounds.count { kindOf(it.characterId) == "friend" }
    val tTotal = rounds.count { kindOf(it.characterId) == "teacher" }
    val fGuessed = rounds.count { kindOf(it.characterId) == "friend" && results[it.playerId] == "guessed" }
    val tGuessed = rounds.count { kindOf(it.characterId) == "teacher" && results[it.playerId] == "guessed" }
    return SummaryStats(
        guessed = guessed,
        total = total,
        best = best,
        friendGuessed = fGuessed,
        friendTotal = fTotal,
        teacherGuessed = tGuessed,
        teacherTotal = tTotal,
        avgShowSec = showMs / 1000
    )
}
