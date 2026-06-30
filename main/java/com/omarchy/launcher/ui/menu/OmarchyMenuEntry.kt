package com.omarchy.launcher.ui.menu

/**
 * One row in the Omarchy-style command menu. Mirrors the structure of
 * the real omarchy-menu shell script (basecamp/omarchy/bin/omarchy-menu):
 * a flat list of icon+label rows per "screen", where selecting a row
 * either runs an action immediately or drills into a child screen.
 */
sealed class OmarchyMenuEntry(
    val icon: String,
    val label: String
) {
    class Action(
        icon: String,
        label: String,
        val onSelect: () -> Unit
    ) : OmarchyMenuEntry(icon, label)

    class Submenu(
        icon: String,
        label: String,
        val children: List<OmarchyMenuEntry>
    ) : OmarchyMenuEntry(icon, label)
}
