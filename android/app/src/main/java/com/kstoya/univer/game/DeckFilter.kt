package com.kstoya.univer.game

/** Фильтр колоды: все, только студенты (друзья), только преподаватели. */
object DeckFilter {
    const val ALL = "all"
    const val FRIENDS = "friends"
    const val TEACHERS = "teachers"

    fun <T> apply(items: List<T>, filter: String, kindOf: (T) -> String): List<T> = when (filter) {
        FRIENDS -> items.filter { kindOf(it) == "friend" }
        TEACHERS -> items.filter { kindOf(it) == "teacher" }
        else -> items
    }

    fun labelRes(filter: String): Int = when (filter) {
        FRIENDS -> com.kstoya.univer.R.string.filter_students
        TEACHERS -> com.kstoya.univer.R.string.filter_teachers
        else -> com.kstoya.univer.R.string.filter_all
    }
}
