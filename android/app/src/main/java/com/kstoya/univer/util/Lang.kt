package com.kstoya.univer.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** Язык хранится синхронно (SharedPreferences), иначе attachBaseContext не успевает. */
object Lang {
    private const val PREFS = "ktya_lang"
    private const val KEY = "lang"
    const val RU = "ru"
    const val EN = "en"

    fun tag(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, RU) ?: RU

    fun set(ctx: Context, tag: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, tag).apply()
    }

    fun wrap(base: Context): Context {
        val locale = Locale(tag(base))
        Locale.setDefault(locale)
        val cfg = Configuration(base.resources.configuration)
        cfg.setLocale(locale)
        cfg.setLayoutDirection(locale)
        return base.createConfigurationContext(cfg)
    }
}
