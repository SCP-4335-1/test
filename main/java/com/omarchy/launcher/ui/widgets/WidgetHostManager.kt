package com.omarchy.launcher.ui.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context

/**
 * Thin wrapper around AppWidgetHost. A launcher must own exactly one
 * AppWidgetHost instance for its lifetime (using a fixed, app-specific
 * HOST_ID) and must call startListening()/stopListening() in step with
 * its own visible lifecycle, or widget RemoteViews updates silently
 * stop arriving. Centralizing that bookkeeping here keeps HomeActivity
 * from having to remember the host's start/stop calls itself.
 */
class WidgetHostManager(context: Context) {

    private val appContext = context.applicationContext
    val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(appContext)
    val appWidgetHost: AppWidgetHost = AppWidgetHost(appContext, HOST_ID)

    fun startListening() = appWidgetHost.startListening()
    fun stopListening() = appWidgetHost.stopListening()

    fun allocateAppWidgetId(): Int = appWidgetHost.allocateAppWidgetId()

    fun deleteAppWidgetId(appWidgetId: Int) {
        appWidgetHost.deleteAppWidgetId(appWidgetId)
    }

    fun createHostView(appWidgetId: Int, providerInfo: AppWidgetProviderInfo): AppWidgetHostView {
        return appWidgetHost.createView(appContext, appWidgetId, providerInfo)
    }

    fun getProviderInfo(appWidgetId: Int): AppWidgetProviderInfo? =
        appWidgetManager.getAppWidgetInfo(appWidgetId)

    fun getInstalledProviders(): List<AppWidgetProviderInfo> =
        appWidgetManager.installedProviders

    companion object {
        // Arbitrary but fixed host id for this launcher; must stay
        // constant across app updates so existing widget bindings
        // remain valid.
        private const val HOST_ID = 4242
    }
}
