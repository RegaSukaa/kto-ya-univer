package com.kstoya.univer.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/** 4 слота реплик: start, guessed, missed, final. Файлы в filesDir/voice/<slot>.m4a. */
class VoicePack(private val ctx: Context) {
    companion object {
        val SLOTS = listOf("start", "guessed", "missed", "final")
        const val MAX_MS = 4000
    }

    private var recorder: MediaRecorder? = null
    private var target: File? = null

    fun dir(): File = File(ctx.filesDir, "voice").apply { mkdirs() }

    fun fileFor(slot: String): File = File(dir(), "$slot.m4a")

    fun has(slot: String): Boolean = fileFor(slot).let { it.exists() && it.length() > 0 }

    fun recordedCount(): Int = SLOTS.count { has(it) }

    fun start(slot: String): File {
        cancel()
        val out = fileFor(slot)
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(ctx) else @Suppress("DEPRECATION") MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioSamplingRate(44100)
        r.setAudioChannels(1)
        r.setAudioEncodingBitRate(96000)
        r.setOutputFile(out.absolutePath)
        r.prepare()
        r.start()
        recorder = r
        target = out
        return out
    }

    fun maxAmplitude(): Int = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)

    fun stop(): File? {
        val r = recorder ?: return null
        val out = target
        runCatching { r.stop() }
        runCatching { r.release() }
        recorder = null
        return if (out != null && out.exists() && out.length() > 0) out else null
    }

    fun cancel() {
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        target?.let { runCatching { it.delete() } }
        target = null
    }

    fun delete(slot: String) {
        runCatching { fileFor(slot).delete() }
    }
}
