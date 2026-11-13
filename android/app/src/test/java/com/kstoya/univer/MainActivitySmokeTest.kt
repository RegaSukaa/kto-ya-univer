package com.kstoya.univer

import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Проверяет, что приложение стартует: Application → ViewModel → NavHost → первый экран. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "ru-rRU-w411dp-h891dp-xxhdpi")
class MainActivitySmokeTest {
    @Test
    fun appLaunches() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        assertNotNull(controller.get())
        controller.pause().stop().destroy()
    }
}
