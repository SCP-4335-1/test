package com.omarchy.launcher.ui.menu

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.omarchy.launcher.util.DeviceLockUtil

/**
 * Builds the top-level Omarchy-menu tree, modeled after the real
 * omarchy-menu shell script's main menu (Apps / Learn / Trigger /
 * Style / Setup / Install / Remove / Update / About / System), but
 * with each leaf mapped to something that actually exists and is
 * meaningful on Android -- there is no package manager or Hyprland
 * config to shell out to here, so categories that don't translate
 * (Install/Remove/Update, which on Arch mean pacman operations) are
 * remapped to their closest Android equivalents instead of omitted,
 * so the menu's shape still matches the original.
 */
class OmarchyMenuBuilder(
    private val context: Context,
    private val onOpenWalkerLauncher: () -> Unit,
    private val onOpenSettings: () -> Unit,
    private val onOpenWallpaperPicker: () -> Unit,
    private val onOpenWidgetPicker: () -> Unit,
    private val onCloseMenu: () -> Unit
) {

    /**
     * "Apps" is a leaf Action here, not a Submenu -- exactly like the
     * real omarchy-menu's go_to_menu() routes *apps*) straight to
     * `walker -p "Launch…"` rather than into a browsable list. Selecting
     * it hands off to the dedicated WalkerAppLauncherView immediately.
     */
    fun buildMainMenu(): List<OmarchyMenuEntry> = listOf(
        OmarchyMenuEntry.Action("▦", "Apps") {
            onCloseMenu()
            onOpenWalkerLauncher()
        },
        OmarchyMenuEntry.Submenu("◐", "Style", buildStyleMenu()),
        OmarchyMenuEntry.Submenu("⚙", "Setup", buildSetupMenu()),
        OmarchyMenuEntry.Submenu("▢", "Widgets", buildWidgetsMenu()),
        OmarchyMenuEntry.Submenu("ℹ", "About", buildAboutMenu()),
        OmarchyMenuEntry.Submenu("⏻", "System", buildSystemMenu())
    )

    private fun buildStyleMenu(): List<OmarchyMenuEntry> = listOf(
        OmarchyMenuEntry.Action("◧", "Wallpaper") {
            onCloseMenu()
            onOpenWallpaperPicker()
        },
        OmarchyMenuEntry.Action("≡", "Launcher settings (launcher.conf)") {
            onCloseMenu()
            onOpenSettings()
        },
        OmarchyMenuEntry.Action("▣", "Display settings") {
            onCloseMenu()
            launchSystemSettings(Settings.ACTION_DISPLAY_SETTINGS)
        }
    )

    private fun buildSetupMenu(): List<OmarchyMenuEntry> = listOf(
        OmarchyMenuEntry.Action("◎", "Wi-Fi") {
            onCloseMenu()
            launchSystemSettings(Settings.ACTION_WIFI_SETTINGS)
        },
        OmarchyMenuEntry.Action("◍", "Bluetooth") {
            onCloseMenu()
            launchSystemSettings(Settings.ACTION_BLUETOOTH_SETTINGS)
        },
        OmarchyMenuEntry.Action("♪", "Sound") {
            onCloseMenu()
            launchSystemSettings(Settings.ACTION_SOUND_SETTINGS)
        },
        OmarchyMenuEntry.Action("▤", "Default apps") {
            onCloseMenu()
            launchSystemSettings(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        },
        OmarchyMenuEntry.Action("⚙", "All settings") {
            onCloseMenu()
            launchSystemSettings(Settings.ACTION_SETTINGS)
        }
    )

    private fun buildWidgetsMenu(): List<OmarchyMenuEntry> = listOf(
        OmarchyMenuEntry.Action("▢", "Add widget…") {
            onCloseMenu()
            onOpenWidgetPicker()
        }
    )

    private fun buildAboutMenu(): List<OmarchyMenuEntry> = listOf(
        OmarchyMenuEntry.Action("ℹ", "omr_launcher -- v0.1.0-alpha") {
            onCloseMenu()
        },
        OmarchyMenuEntry.Action("▤", "App info") {
            onCloseMenu()
            launchSystemSettings(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, asPackageUri = true)
        }
    )

    private fun buildSystemMenu(): List<OmarchyMenuEntry> = listOf(
        OmarchyMenuEntry.Action("⏻", "Lock") {
            onCloseMenu()
            DeviceLockUtil.lockNow(context)
        }
    )

    private fun launchSystemSettings(action: String, asPackageUri: Boolean = false) {
        try {
            val intent = Intent(action)
            if (asPackageUri) {
                intent.data = Uri.parse("package:${context.packageName}")
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // Setting screen not available on this device/OEM skin; no-op
            // rather than crashing the whole launcher over a missing intent.
        }
    }
}
