package com.kstoya.univer.game

import java.util.Random

/**
 * Единственная раздача на оба режима: детерминирована по seed,
 * поэтому серверу достаточно передать seed и порядок карточек.
 */
object Deck {

    fun deal(
        seed: Long,
        deck: List<String>,
        players: List<String>,
        alternateKinds: Boolean = true,
        kindOf: (String) -> String = { "other" }
    ): Map<String, String> {
        if (deck.isEmpty() || players.isEmpty()) return emptyMap()
        val rnd = Random(seed)

        fun shuffled(list: List<String>): List<String> {
            val a = list.toMutableList()
            for (i in a.lastIndex downTo 1) {
                val j = rnd.nextInt(i + 1)
                val tmp = a[i]; a[i] = a[j]; a[j] = tmp
            }
            return a
        }

        val friends = shuffled(deck.filter { kindOf(it) == "friend" })
        val teachers = shuffled(deck.filter { kindOf(it) == "teacher" })
        val rest = shuffled(deck.filter { kindOf(it) != "friend" && kindOf(it) != "teacher" })

        val ordered = ArrayList<String>(deck.size)
        if (alternateKinds && friends.isNotEmpty() && teachers.isNotEmpty()) {
            var f = 0
            var t = 0
            var takeFriend = friends.size >= teachers.size
            while (f < friends.size || t < teachers.size) {
                when {
                    takeFriend && f < friends.size -> { ordered += friends[f++] }
                    !takeFriend && t < teachers.size -> { ordered += teachers[t++] }
                    f < friends.size -> { ordered += friends[f++] }
                    t < teachers.size -> { ordered += teachers[t++] }
                }
                takeFriend = !takeFriend
            }
        } else {
            ordered += friends
            ordered += teachers
        }
        ordered += rest

        val result = LinkedHashMap<String, String>()
        players.forEachIndexed { i, player ->
            val idx = i % ordered.size
            var candidate = ordered[idx]
            if (i > 0 && ordered.size > 1) {
                val prev = result[players[i - 1]]
                var shift = 1
                while (candidate == prev && shift < ordered.size) {
                    candidate = ordered[(idx + shift) % ordered.size]
                    shift++
                }
            }
            result[player] = candidate
        }
        return result
    }

    /** Перемешивание порядка игроков тем же PRNG (нужно для одинакового порядка на всех клиентах). */
    fun shufflePlayers(seed: Long, players: List<String>): List<String> {
        val rnd = Random(seed)
        val a = players.toMutableList()
        for (i in a.lastIndex downTo 1) {
            val j = rnd.nextInt(i + 1)
            val tmp = a[i]; a[i] = a[j]; a[j] = tmp
        }
        return a
    }
}
