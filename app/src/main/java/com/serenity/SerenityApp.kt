package com.serenity

import android.app.Application
import com.serenity.crash.CrashHandler
import com.serenity.crash.LogCollector
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SerenityApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Install crash handler first — captures any crash including Hilt init failures
        CrashHandler.install(this)
        // Start logcat collector — captures Compose/Hilt warnings that aren't fatal exceptions
        LogCollector.start(this)
    }
}
