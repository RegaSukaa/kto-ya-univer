package com.kstoya.univer.audio

enum class SoundSource { PERSONAL, BANK, ASSET }

/** Порядок выбора: личная реплика → случайный голос из банка → встроенный WAV. Тишины нет. */
object SoundPriority {
    fun resolve(personalExists: Boolean, bankExists: Boolean): SoundSource = when {
        personalExists -> SoundSource.PERSONAL
        bankExists -> SoundSource.BANK
        else -> SoundSource.ASSET
    }
}
