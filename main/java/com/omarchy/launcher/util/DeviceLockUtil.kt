package com.omarchy.launcher.util

import android.app.admin.DevicePolicyManager
import android.content.Context

/**
 * Double-tap-to-lock requires either:
 *  (a) device admin (DevicePolicyManager.lockNow()) which needs the user
 *      to explicitly activate this app as a device admin in Settings, or
 *  (b) an AccessibilityService with GLOBAL_ACTION_LOCK_SCREEN (API 28+).
 *
 * Neither can be silently granted -- there is no public API for a
 * regular launcher to lock the screen without one of these explicit
 * user grants. This helper makes the device-admin path available and
 * degrades gracefully (no-op) if the permission hasn't been granted yet,
 * rather than crashing.
 */
object DeviceLockUtil {

    fun canLock(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        return dpm?.isAdminActive(adminComponent(context)) == true
    }

    fun lockNow(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            ?: return false
        return if (dpm.isAdminActive(adminComponent(context))) {
            dpm.lockNow()
            true
        } else {
            false
        }
    }

    private fun adminComponent(context: Context) =
        android.content.ComponentName(context, "com.omarchy.launcher.util.LauncherDeviceAdminReceiver")
}
