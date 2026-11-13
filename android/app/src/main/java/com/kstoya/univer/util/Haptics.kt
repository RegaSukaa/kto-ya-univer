package com.kstoya.univer.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object Haptics {
    private fun vib(ctx: Context): Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= 31) {
            (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }.getOrNull()

    fun pattern(ctx: Context, enabled: Boolean, pattern: LongArray) {
        if (!enabled) return
        val v = vib(ctx) ?: return
        runCatching { v.vibrate(VibrationEffect.createWaveform(pattern, -1)) }
    }

    fun start(ctx: Context, enabled: Boolean) = pattern(ctx, enabled, longArrayOf(0, 60, 80, 60, 80, 120))
    fun success(ctx: Context, enabled: Boolean) = pattern(ctx, enabled, longArrayOf(0, 120))
    fun fail(ctx: Context, enabled: Boolean) = pattern(ctx, enabled, longArrayOf(0, 40, 60, 40))
    fun tick(ctx: Context, enabled: Boolean) = pattern(ctx, enabled, longArrayOf(0, 25))
}
