package com.lee.vestige

import android.app.Application
import com.lee.vestige.di.AppContainer

/** Holds the app-wide [AppContainer]. */
class VestigeApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
