package com.kstoya.univer.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kstoya.univer.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("ktya_settings")

data class Settings(
    val showMs: Int = 10_000,
    val alternateKinds: Boolean = true,
    val markAfter: Boolean = false,
    val sound: Boolean = true,
    val vibrate: Boolean = true,
    val tapHide: Boolean = true,
    val voiceVolume: Float = 1f,
    val serverUrl: String = BuildConfig.SERVER_URL,
    val darkTheme: Boolean = true
)

class Prefs(private val context: Context) {
    private object K {
        val SHOW_MS = intPreferencesKey("show_ms")
        val ALTERNATE = booleanPreferencesKey("alternate")
        val MARK_AFTER = booleanPreferencesKey("mark_after")
        val SOUND = booleanPreferencesKey("sound")
        val VIBRATE = booleanPreferencesKey("vibrate")
        val TAP_HIDE = booleanPreferencesKey("tap_hide")
        val VOICE_VOL = floatPreferencesKey("voice_volume")
        val SERVER = stringPreferencesKey("server_url")
        val DARK = booleanPreferencesKey("dark_theme")
    }

    val settings: Flow<Settings> get() = context.dataStore.data.map { p ->
        Settings(
            showMs = p[K.SHOW_MS] ?: 10_000,
            alternateKinds = p[K.ALTERNATE] ?: true,
            markAfter = p[K.MARK_AFTER] ?: false,
            sound = p[K.SOUND] ?: true,
            vibrate = p[K.VIBRATE] ?: true,
            tapHide = p[K.TAP_HIDE] ?: true,
            voiceVolume = p[K.VOICE_VOL] ?: 1f,
            serverUrl = p[K.SERVER] ?: BuildConfig.SERVER_URL,
            darkTheme = p[K.DARK] ?: true
        )
    }

    suspend fun setShowMs(v: Int) = context.dataStore.edit { it[K.SHOW_MS] = v }
    suspend fun setAlternate(v: Boolean) = context.dataStore.edit { it[K.ALTERNATE] = v }
    suspend fun setMarkAfter(v: Boolean) = context.dataStore.edit { it[K.MARK_AFTER] = v }
    suspend fun setSound(v: Boolean) = context.dataStore.edit { it[K.SOUND] = v }
    suspend fun setVibrate(v: Boolean) = context.dataStore.edit { it[K.VIBRATE] = v }
    suspend fun setTapHide(v: Boolean) = context.dataStore.edit { it[K.TAP_HIDE] = v }
    suspend fun setVoiceVolume(v: Float) = context.dataStore.edit { it[K.VOICE_VOL] = v }
    suspend fun setServerUrl(v: String) = context.dataStore.edit { it[K.SERVER] = v }
    suspend fun setDarkTheme(v: Boolean) = context.dataStore.edit { it[K.DARK] = v }
}
