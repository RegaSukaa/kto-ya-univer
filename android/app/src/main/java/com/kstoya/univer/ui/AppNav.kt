package com.kstoya.univer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kstoya.univer.AppViewModel
import com.kstoya.univer.ui.screens.AboutScreen
import com.kstoya.univer.ui.screens.GameHost
import com.kstoya.univer.ui.screens.HomeScreen
import com.kstoya.univer.ui.screens.LibraryScreen
import com.kstoya.univer.ui.screens.OnboardingScreen
import com.kstoya.univer.ui.screens.OnlineScreen
import com.kstoya.univer.ui.screens.PlaceholderScreen
import com.kstoya.univer.ui.screens.RulesScreen
import com.kstoya.univer.ui.screens.SettingsScreen
import com.kstoya.univer.ui.screens.SetupScreen

private const val UI_PREFS = "ktya_ui"
const val KEY_ONBOARDED = "onboarded"

fun markOnboarded(ctx: android.content.Context) {
    ctx.getSharedPreferences(UI_PREFS, 0).edit().putBoolean(KEY_ONBOARDED, true).apply()
}

@Composable
fun AppNav(vm: AppViewModel) {
    val nav = rememberNavController()
    val ctx = LocalContext.current
    val start = if (ctx.getSharedPreferences(UI_PREFS, 0).getBoolean(KEY_ONBOARDED, false)) "home" else "onboarding"

    NavHost(navController = nav, startDestination = start) {
        composable("onboarding") {
            OnboardingScreen(
                onDone = { goLibrary ->
                    markOnboarded(ctx)
                    if (goLibrary) nav.navigate("library") { popUpTo("onboarding") { inclusive = true } }
                    else nav.navigate("home") { popUpTo("onboarding") { inclusive = true } }
                }
            )
        }
        composable("home") {
            val chars by vm.characters.collectAsState()
            HomeScreen(
                characters = chars,
                onLocal = { nav.navigate("setup") },
                onOnline = { nav.navigate("online") },
                onLibrary = { nav.navigate("library") },
                onSettings = { nav.navigate("settings") },
                onRules = { nav.navigate("rules") }
            )
        }
        composable("rules") { RulesScreen(onBack = { nav.popBackStack() }) }
        composable("setup") {
            val chars by vm.characters.collectAsState()
            val settings by vm.settings.collectAsState()
            val presets by vm.presets.collectAsState()
            SetupScreen(
                characters = chars,
                settings = settings,
                presets = presets,
                presetNames = { vm.presetNameList(it) },
                onBack = { nav.popBackStack() },
                onSavePreset = { title, names -> vm.savePreset(title, names) },
                onStart = { names, showMs, filter ->
                    vm.playClick()
                    if (vm.startLocal(names, showMs, filter)) {
                        nav.navigate("game") { popUpTo("home") }
                    }
                },
                onRules = { nav.navigate("rules") }
            )
        }
        composable("game") {
            GameHost(vm = vm, onExit = {
                vm.closeGame()
                nav.popBackStack("home", false)
            })
        }
        composable("library") {
            val chars by vm.characters.collectAsState()
            LibraryScreen(
                characters = chars,
                onBack = { nav.popBackStack() },
                onExport = { list, title -> vm.exportFile(list, title) },
                onImport = { uri, onPreview -> vm.importPreview(uri, onPreview) },
                onApplyImport = { preview, policy, done -> vm.applyImport(preview, policy, done) }
            )
        }
        composable("settings") {
            val settings by vm.settings.collectAsState()
            val chars by vm.characters.collectAsState()
            SettingsScreen(
                settings = settings,
                language = vm.language(),
                onBack = { nav.popBackStack() },
                onShowMs = { vm.setShowMs(it) },
                onSound = { vm.setSound(it) },
                onVibrate = { vm.setVibrate(it) },
                onTapHide = { vm.setTapHide(it) },
                onVoiceVolume = { vm.setVoiceVolume(it) },
                onDarkTheme = { vm.setDarkTheme(it) },
                onLanguage = { tag ->
                    vm.setLanguage(tag)
                    ctx.findActivity()?.recreate()
                },
                onDeckFilter = { vm.setDeckFilter(it) },
                onPlaceholders = { nav.navigate("placeholders") },
                onAbout = { nav.navigate("about") },
                onWipe = { done -> vm.wipeData { done() } },
                onExport = { file -> com.kstoya.univer.util.Share.file(ctx, file, "application/zip", "KtoYa set") },
                onExportSet = { vm.exportFile(chars, ctx.getString(com.kstoya.univer.R.string.share_subject)) },
                onImport = { uri, onPreview -> vm.importPreview(uri, onPreview) },
                onApplyImport = { preview, policy, done -> vm.applyImport(preview, policy, done) }
            )
        }
        composable("placeholders") {
            val chars by vm.characters.collectAsState()
            PlaceholderScreen(
                characters = chars,
                onBack = { nav.popBackStack() },
                onRefresh = { cb -> vm.refreshBank(cb) }
            )
        }
        composable("about") { AboutScreen(onBack = { nav.popBackStack() }) }
        composable("online") {
            val chars by vm.characters.collectAsState()
            val settings by vm.settings.collectAsState()
            OnlineScreen(
                characters = chars,
                settings = settings,
                onBack = { nav.popBackStack() },
                onSaveServer = { vm.setServerUrl(it) }
            )
        }
    }
}

private fun android.content.Context.findActivity(): android.app.Activity? {
    var c: android.content.Context? = this
    while (c != null) {
        if (c is android.app.Activity) return c
        c = (c as? android.content.ContextWrapper)?.baseContext
    }
    return null
}
