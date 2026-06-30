package com.omarchy.launcher.ui.wallpaper

/**
 * A small set of built-in, programmatically-drawn wallpapers in the
 * Omarchy/terminal palette. No bundled image assets exist in this
 * project, so each preset is a gradient + accent pair that
 * WallpaperPresetRenderer turns into a Bitmap on demand.
 */
data class WallpaperPreset(
    val id: String,
    val label: String,
    val topColor: Int,
    val bottomColor: Int,
    val accentColor: Int
)

object WallpaperPresets {
    val all: List<WallpaperPreset> = listOf(
        WallpaperPreset("void_cyan", "Void / Cyan", 0xFF0A0E14.toInt(), 0xFF10151C.toInt(), 0xFF00F5FF.toInt()),
        WallpaperPreset("void_magenta", "Void / Magenta", 0xFF0A0E14.toInt(), 0xFF1A0F1C.toInt(), 0xFFFF2BD6.toInt()),
        WallpaperPreset("void_green", "Void / Green", 0xFF0A0E14.toInt(), 0xFF0E1A12.toInt(), 0xFF39FF6A.toInt()),
        WallpaperPreset("void_amber", "Void / Amber", 0xFF0A0E14.toInt(), 0xFF1C150A.toInt(), 0xFFFFB627.toInt())
    )
}
