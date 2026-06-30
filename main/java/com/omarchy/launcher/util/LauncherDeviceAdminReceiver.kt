package com.omarchy.launcher.util

import android.app.admin.DeviceAdminReceiver

/**
 * Minimal DeviceAdminReceiver. We don't actually need any callbacks here --
 * its only job is to exist as a valid admin component so
 * DevicePolicyManager.lockNow() can be called once the user has granted
 * device-admin rights to this launcher from Settings.
 */
class LauncherDeviceAdminReceiver : DeviceAdminReceiver()
