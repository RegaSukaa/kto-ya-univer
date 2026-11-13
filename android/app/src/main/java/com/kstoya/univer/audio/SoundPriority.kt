package com.kstoya.univer.audio

enum class SoundSource { PERSONAL, VOICE_PACK, ASSET }

/** Порядок выбора звука: личная реплика → голосовой пак → встроенный wav. Тишины нет ни в одном случае. */
object SoundPriority {
    fun resolve(personalExists: Boolean, voicePackExists: Boolean): SoundSource = when {
        personalExists -> SoundSource.PERSONAL
        voicePackExists -> SoundSource.VOICE_PACK
        else -> SoundSource.ASSET
    }
}
