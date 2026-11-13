package com.kstoya.univer.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.util.Log
import com.kstoya.univer.data.Settings
import java.io.File

/**
 * Короткие сэмплы — SoundPool (задержка < 50 мс), реплики — MediaPlayer.
 * Пустой/битый файл не роняет воспроизведение: играет следующий в цепочке.
 */
class Sounds(private val ctx: Context) {
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

    fun voicePackFile(slot: String): File = File(File(ctx.filesDir, "voice"), "$slot.m4a")

    /**
     * Реплика: assets/<id>_<slot>.m4a → filesDir/voice/<slot>.m4a → assets/sounds/<slot>.wav.
     * slot: win | skip | start | final
     */
    fun playVoice(characterId: String?, slot: String, settings: Settings) {
        if (!settings.sound || streamVolume() <= 0) return
        val personal = characterId?.let { "sounds/${it}_$slot.m4a" }
        if (personal != null && assetSounds.contains("${characterId}_$slot.m4a")) {
            if (playAssetMp3(personal, settings.voiceVolume)) return
        }
        val recorded = voicePackFile(slot)
        if (recorded.exists() && recorded.length() > 0) {
            if (playFile(recorded, settings.voiceVolume)) return
        }
        val fallback = when (slot) {
            "win" -> "win"
            "skip" -> "skip"
            "start" -> "go"
            "final" -> "fanfare"
            else -> "win"
        }
        play(fallback, settings.voiceVolume, settings)
    }

    private fun streamVolume(): Int = runCatching {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.getStreamVolume(AudioManager.STREAM_MUSIC)
    }.getOrDefault(1)

    private fun playAssetMp3(path: String, volume: Float): Boolean = runCatching {
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

    fun playFile(file: File, volume: Float = 1f): Boolean = runCatching {
        releasePlayer()
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
            )
            setDataSource(file.absolutePath)
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
