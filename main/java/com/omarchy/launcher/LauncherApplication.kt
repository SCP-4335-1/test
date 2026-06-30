package com.omarchy.launcher

import android.app.Application
import com.omarchy.launcher.data.AppRepository
import com.omarchy.launcher.data.LayoutStore

/**
 * Holds the small set of app-wide singletons. Deliberately not using a
 * DI framework (Hilt/Koin) here -- the object graph is tiny (two
 * classes) and a launcher app benefits from starting as fast as
 * possible, so we skip the DI startup overhead entirely.
 */
class LauncherApplication : Application() {

    lateinit var appRepository: AppRepository
        private set

    lateinit var layoutStore: LayoutStore
        private set

    override fun onCreate() {
        super.onCreate()
        appRepository = AppRepository(this)
        layoutStore = LayoutStore(this)
    }

    companion object {
        fun from(context: android.content.Context): LauncherApplication =
            context.applicationContext as LauncherApplication
    }
}
