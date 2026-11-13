package com.kstoya.univer

import android.app.Application
import android.content.Context
import com.kstoya.univer.audio.Sounds
import com.kstoya.univer.data.AppDb
import com.kstoya.univer.data.Prefs
import com.kstoya.univer.data.Repo
import com.kstoya.univer.util.Lang
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class App : Application() {
    lateinit var repo: Repo
        private set
    lateinit var prefs: Prefs
        private set
    lateinit var sounds: Sounds
        private set
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(Lang.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        repo = Repo(this, AppDb.get(this), prefs)
        sounds = Sounds(this)
        // В Robolectric база и assets живут недолго: фоновое наполнение ломает SQLite-песочницу.
        if (!android.os.Build.FINGERPRINT.contains("robolectric")) {
            scope.launch { runCatching { repo.seedFromAssets() } }
        }
    }
}
