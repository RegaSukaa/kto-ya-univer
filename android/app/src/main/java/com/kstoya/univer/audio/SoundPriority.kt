package com.kstoya.univer.audio

enum class SoundSource { BANK, ASSET }

/** Случайный голос из банка → встроенный WAV. Тишины нет. */
object SoundPriority {
    fun resolve(bankExists: Boolean): SoundSource = if (bankExists) SoundSource.BANK else SoundSource.ASSET
}
