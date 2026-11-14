package com.kstoya.univer.game

enum class LocalPhase { PASS, COUNTDOWN, SHOW, REVIEW }

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
 * Механика без счёта: передача → отсчёт → карточка → следующий.
 * Результаты не фиксируются, «угадал/не угадал» в приложении нет —
 * обсуждение и вопросы происходят вживую.
 */
class LocalGameEngine(
    val players: List<PlayerSlot>,
    val rounds: List<RoundInfo>,
    val showMs: Int
) {
    var phase: LocalPhase = LocalPhase.PASS
        private set
    var index: Int = 0
        private set
    var phaseStart: Long = 0L
        private set
    var finished: Boolean = false
        private set

    val current: RoundInfo get() = rounds[index.coerceIn(0, (rounds.size - 1).coerceAtLeast(0))]

    fun start(now: Long) {
        phase = LocalPhase.PASS
        index = 0
        phaseStart = now
        finished = false
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
                        advance(end)
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
    fun countdownDigit(now: Long): Int = digitFor(now - phaseStart)

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

    fun finish(now: Long) {
        finished = true
        phase = LocalPhase.REVIEW
        phaseStart = now
    }
}

fun digitFor(elapsed: Long): Int = when {
    elapsed < COUNT_STEP_MS -> 3
    elapsed < 2 * COUNT_STEP_MS -> 2
    elapsed < 3 * COUNT_STEP_MS -> 1
    else -> 0
}
