package com.kstoya.univer.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kstoya.univer.BuildConfig
import com.kstoya.univer.game.DeckFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("ktya_settings")

data class Settings(
    val showMs: Int = 10_000,
    val sound: Boolean = true,
    val vibrate: Boolean = true,
    val tapHide: Boolean = true,
    val voiceVolume: Float = 1f,
    val serverUrl: String = BuildConfig.SERVER_URL,
    val darkTheme: Boolean = true,
    /** all | friends | teachers */
    val deckFilter: String = DeckFilter.ALL
)

class Prefs(private val context: Context) {
    private object K {
        val SHOW_MS = intPreferencesKey("show_ms")
        val SOUND = booleanPreferencesKey("sound")
        val VIBRATE = booleanPreferencesKey("vibrate")
        val TAP_HIDE = booleanPreferencesKey("tap_hide")
        val VOICE_VOL = floatPreferencesKey("voice_volume")
        val SERVER = stringPreferencesKey("server_url")
        val DARK = booleanPreferencesKey("dark_theme")
        val DECK_FILTER = stringPreferencesKey("deck_filter")
    }

    val settings: Flow<Settings> get() = context.dataStore.data.map { p ->
        Settings(
            showMs = p[K.SHOW_MS] ?: 10_000,
            sound = p[K.SOUND] ?: true,
            vibrate = p[K.VIBRATE] ?: true,
            tapHide = p[K.TAP_HIDE] ?: true,
            voiceVolume = p[K.VOICE_VOL] ?: 1f,
            serverUrl = p[K.SERVER]
                ?.takeIf { it.isNotBlank() && !it.contains("10.0.2.2") && !it.contains("localhost") && !it.contains("127.0.0.1") }
                ?: BuildConfig.SERVER_URL,
            darkTheme = p[K.DARK] ?: true,
            deckFilter = p[K.DECK_FILTER] ?: DeckFilter.ALL
        )
    }

    suspend fun setShowMs(v: Int) = context.dataStore.edit { it[K.SHOW_MS] = v }
    suspend fun setSound(v: Boolean) = context.dataStore.edit { it[K.SOUND] = v }
    suspend fun setVibrate(v: Boolean) = context.dataStore.edit { it[K.VIBRATE] = v }
    suspend fun setTapHide(v: Boolean) = context.dataStore.edit { it[K.TAP_HIDE] = v }
    suspend fun setVoiceVolume(v: Float) = context.dataStore.edit { it[K.VOICE_VOL] = v }
    suspend fun setServerUrl(v: String) = context.dataStore.edit { it[K.SERVER] = v }
    suspend fun setDarkTheme(v: Boolean) = context.dataStore.edit { it[K.DARK] = v }
    suspend fun setDeckFilter(v: String) = context.dataStore.edit { it[K.DECK_FILTER] = v }
}
