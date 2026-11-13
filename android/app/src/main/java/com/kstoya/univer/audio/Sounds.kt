package com.kstoya.univer.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import com.kstoya.univer.data.Settings

/**
 * Голоса — заранее записанные файлы из assets/voices/<slot>/, выбираются случайно.
 * Если банк пуст — встроенный WAV. Тишины нет ни в одном случае.
 */
class Sounds(private val ctx: Context) {
    companion object {
        val VOICE_SLOTS = listOf("go", "guessed", "missed", "final")
    }

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        ).build()

    private val ids = mutableMapOf<String, Int>()
    private var player: MediaPlayer? = null
    private val bank: Map<String, List<String>> by lazy {
        VOICE_SLOTS.associateWith { slot ->
            runCatching {
                ctx.assets.list("voices/$slot")
                    ?.filter { it.endsWith(".m4a", true) || it.endsWith(".wav", true) }
                    ?.sorted()
                    ?: emptyList()
            }.getOrDefault(emptyList())
        }
    }
    private val lastVoice = mutableMapOf<String, String>()
    private val assetSounds: Set<String> by lazy {
        runCatching { ctx.assets.list("sounds")?.toSet() ?: emptySet() }.getOrDefault(emptySet())
    }

    init {
        listOf("go", "tick", "tick2", "tick3", "time_up", "win", "skip", "click", "fanfare", "ding").forEach { name ->
            runCatching { ids[name] = ctx.assets.openFd("sounds/$name.wav").use { pool.load(it, 1) } }
        }
    }

    fun play(name: String, volume: Float = 1f, settings: Settings) {
        if (!settings.sound) return
        if (streamVolume() <= 0) return
        val id = ids[name] ?: return
        runCatching { pool.play(id, volume, volume, 1, 0, 1f) }
    }

    fun hasPersonal(characterId: String, slot: String): Boolean =
        assetSounds.contains("${characterId}_$slot.m4a")

    fun hasBank(slot: String): Boolean = bank[slot]?.isNotEmpty() == true

    /** Случайная реплика из банка, с защитой от повтора подряд. */
    fun playBank(slot: String, volume: Float): Boolean {
        val files = bank[slot] ?: return false
        if (files.isEmpty()) return false
        val candidates = if (files.size > 1) files.filter { it != lastVoice[slot] } else files
        val pick = candidates.random()
        lastVoice[slot] = pick
        return playAsset("voices/$slot/$pick", volume)
    }

    /**
     * Реплика: личный голос персонажа (assets/sounds/<id>_win.m4a) →
     * случайный голос из банка (assets/voices/<slot>/) → встроенный WAV.
     */
    fun playVoice(characterId: String?, slot: String, settings: Settings) {
        if (!settings.sound || streamVolume() <= 0) return
        val personal = when (slot) {
            "guessed" -> "win"
            "missed" -> "skip"
            else -> null
        }
        if (characterId != null && personal != null) {
            val name = "${characterId}_$personal.m4a"
            if (assetSounds.contains(name) && playAsset("sounds/$name", settings.voiceVolume)) return
        }
        if (playBank(slot, settings.voiceVolume)) return
        val fallback = when (slot) {
            "go" -> "go"
            "guessed" -> "win"
            "missed" -> "skip"
            "final" -> "fanfare"
            else -> "win"
        }
        play(fallback, settings.voiceVolume, settings)
    }

    private fun streamVolume(): Int = runCatching {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.getStreamVolume(AudioManager.STREAM_MUSIC)
    }.getOrDefault(1)

    private fun playAsset(path: String, volume: Float): Boolean = runCatching {
        releasePlayer()
        val afd = ctx.assets.openFd(path)
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
            )
            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            setVolume(volume, volume)
            prepare()
            start()
        }
        true
    }.getOrElse { false }

    fun releasePlayer() {
        runCatching {
            player?.let { if (it.isPlaying) it.stop(); it.release() }
        }
        player = null
    }

    fun release() {
        releasePlayer()
        runCatching { pool.release() }
    }
}
