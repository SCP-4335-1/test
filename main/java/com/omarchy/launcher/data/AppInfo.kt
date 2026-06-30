package com.omarchy.launcher.data

import android.graphics.drawable.Drawable
import android.os.UserHandle

/**
 * Lightweight representation of an installed, launchable app.
 * Icons are resolved lazily and cached by [AppRepository] to avoid
 * holding huge Drawable graphs for the whole app list at once.
 */
data class AppInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
    val userHandle: UserHandle,
    var icon: Drawable? = null
) {
    /** Stable key across the launcher (package + activity + user). */
    val key: String
        get() = "$packageName/$activityName/${userHandle.hashCode()}"
}
