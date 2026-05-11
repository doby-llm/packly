package com.packly.app

import android.app.Application
import com.packly.app.di.AppModule

class PacklyApplication : Application() {

    lateinit var appModule: AppModule
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        appModule = AppModule(this)
        // Seed default data on first launch
        appModule.seedOnFirstLaunch()
    }

    companion object {
        lateinit var instance: PacklyApplication
            private set
    }
}
