package com.omarchy.launcher.data

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import androidx.collection.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Source of truth for "what apps exist on this device". Uses the
 * LauncherApps system service rather than PackageManager.queryIntentActivities,
 * because LauncherApps is the API actually designed for launcher apps:
 * it understands work-profile / multi-user setups and can notify us
 * when packages are added, removed, or changed without a manual poll.
 */
class AppRepository(private val context: Context) {

    private val launcherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    private val iconCache = LruCache<String, android.graphics.drawable.Drawable>(200)

    private val listeners = mutableListOf<() -> Unit>()

    init {
        launcherApps.registerCallback(object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String?, user: UserHandle?) = notifyChanged()
            override fun onPackageAdded(packageName: String?, user: UserHandle?) = notifyChanged()
            override fun onPackageChanged(packageName: String?, user: UserHandle?) = notifyChanged()
            override fun onPackagesAvailable(
                packageNames: Array<out String>?,
                user: UserHandle?,
                replacing: Boolean
            ) = notifyChanged()

            override fun onPackagesUnavailable(
                packageNames: Array<out String>?,
                user: UserHandle?,
                replacing: Boolean
            ) = notifyChanged()
        })
    }

    fun addOnChangedListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    private fun notifyChanged() {
        listeners.forEach { it() }
    }

    /**
     * Loads all launchable activities across all profiles (main user +
     * any work profile) the launcher has access to. Run off the main
     * thread -- LauncherApps#getActivityList does I/O.
     */
    suspend fun loadAllApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val result = mutableListOf<AppInfo>()
        val profiles = launcherApps.profiles ?: listOf(Process.myUserHandle())

        for (profile in profiles) {
            val activities = launcherApps.getActivityList(null, profile)
            for (activity in activities) {
                val pkg = activity.applicationInfo.packageName
                val cls = activity.componentName.className
                val cacheKey = "$pkg/$cls/${profile.hashCode()}"

                val cachedIcon = iconCache.get(cacheKey)
                val icon = cachedIcon ?: try {
                    activity.getIcon(context.resources.displayMetrics.densityDpi).also {
                        if (it != null) iconCache.put(cacheKey, it)
                    }
                } catch (e: Exception) {
                    null
                }

                result.add(
                    AppInfo(
                        packageName = pkg,
                        activityName = cls,
                        label = activity.label?.toString() ?: pkg,
                        userHandle = profile,
                        icon = icon
                    )
                )
            }
        }

        result.sortedBy { it.label.lowercase() }
    }

    fun launch(app: AppInfo, sourceBounds: android.graphics.Rect? = null) {
        try {
            val component = android.content.ComponentName(app.packageName, app.activityName)
            launcherApps.startMainActivity(component, app.userHandle, sourceBounds, null)
        } catch (e: Exception) {
            // Activity might have been uninstalled between list-load and tap;
            // fail silently rather than crashing the launcher itself.
        }
    }

    fun openAppInfo(app: AppInfo) {
        try {
            val component = android.content.ComponentName(app.packageName, app.activityName)
            launcherApps.startAppDetailsActivity(component, app.userHandle, null, null)
        } catch (e: Exception) {
            // ignore
        }
    }
}
