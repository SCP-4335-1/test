package com.omarchy.launcher.data

/**
 * What kind of thing occupies a cell (or cell-span) on the home grid.
 */
enum class HomeItemType {
    APP,
    FOLDER,
    WIDGET
}

/**
 * A single placed item on a homescreen page. Position is grid-cell based
 * (col/row), not pixel based, so layouts survive density/resolution
 * changes across devices -- this mirrors how Nova Launcher persists
 * its grid.
 */
data class HomeItem(
    val id: String,
    val type: HomeItemType,
    val page: Int,
    val col: Int,
    val row: Int,
    val spanX: Int = 1,
    val spanY: Int = 1,
    // APP
    val packageName: String? = null,
    val activityName: String? = null,
    val label: String? = null,
    // FOLDER
    val folderItemIds: MutableList<String> = mutableListOf(),
    // WIDGET
    val appWidgetId: Int = -1,
    val widgetProviderPackage: String? = null
)
