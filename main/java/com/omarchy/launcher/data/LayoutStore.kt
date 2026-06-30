package com.omarchy.launcher.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Stores and restores the homescreen grid layout (icons, folders, widget
 * placements) and the dock. Backed by SharedPreferences with a hand-rolled
 * JSON schema -- intentionally not a database, since the entire dataset
 * is small (a few hundred items at most) and this also makes manual
 * backup/restore (export/import a single string) trivial, matching the
 * "Icon-Backup/Restore" behavior Nova Launcher is known for.
 */
class LayoutStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveHomeItems(items: List<HomeItem>) {
        val arr = JSONArray()
        items.forEach { arr.put(itemToJson(it)) }
        prefs.edit().putString(KEY_HOME_ITEMS, arr.toString()).apply()
    }

    fun loadHomeItems(): List<HomeItem> {
        val raw = prefs.getString(KEY_HOME_ITEMS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { itemFromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveDockItems(items: List<HomeItem>) {
        val arr = JSONArray()
        items.forEach { arr.put(itemToJson(it)) }
        prefs.edit().putString(KEY_DOCK_ITEMS, arr.toString()).apply()
    }

    fun loadDockItems(): List<HomeItem> {
        val raw = prefs.getString(KEY_DOCK_ITEMS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { itemFromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun savePageCount(count: Int) {
        prefs.edit().putInt(KEY_PAGE_COUNT, count).apply()
    }

    fun loadPageCount(): Int = prefs.getInt(KEY_PAGE_COUNT, 1)

    /** Serializes the whole layout (home + dock + page count) to one portable string. */
    fun exportLayout(): String {
        val root = JSONObject()
        val homeArr = JSONArray()
        loadHomeItems().forEach { homeArr.put(itemToJson(it)) }
        val dockArr = JSONArray()
        loadDockItems().forEach { dockArr.put(itemToJson(it)) }
        root.put("home", homeArr)
        root.put("dock", dockArr)
        root.put("pages", loadPageCount())
        root.put("schema", SCHEMA_VERSION)
        return root.toString()
    }

    /** Restores a layout previously produced by [exportLayout]. Returns success flag. */
    fun importLayout(serialized: String): Boolean {
        return try {
            val root = JSONObject(serialized)
            val homeArr = root.getJSONArray("home")
            val dockArr = root.getJSONArray("dock")
            val pages = root.optInt("pages", 1)

            saveHomeItems((0 until homeArr.length()).map { itemFromJson(homeArr.getJSONObject(it)) })
            saveDockItems((0 until dockArr.length()).map { itemFromJson(dockArr.getJSONObject(it)) })
            savePageCount(pages)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun itemToJson(item: HomeItem): JSONObject = JSONObject().apply {
        put("id", item.id)
        put("type", item.type.name)
        put("page", item.page)
        put("col", item.col)
        put("row", item.row)
        put("spanX", item.spanX)
        put("spanY", item.spanY)
        item.packageName?.let { put("packageName", it) }
        item.activityName?.let { put("activityName", it) }
        item.label?.let { put("label", it) }
        put("folderItemIds", JSONArray(item.folderItemIds))
        put("appWidgetId", item.appWidgetId)
        item.widgetProviderPackage?.let { put("widgetProviderPackage", it) }
    }

    private fun itemFromJson(json: JSONObject): HomeItem {
        val folderIds = mutableListOf<String>()
        json.optJSONArray("folderItemIds")?.let { arr ->
            for (i in 0 until arr.length()) folderIds.add(arr.getString(i))
        }
        fun optStringOrNull(key: String): String? =
            if (json.isNull(key) || !json.has(key)) null else json.optString(key)

        return HomeItem(
            id = json.getString("id"),
            type = HomeItemType.valueOf(json.getString("type")),
            page = json.getInt("page"),
            col = json.getInt("col"),
            row = json.getInt("row"),
            spanX = json.optInt("spanX", 1),
            spanY = json.optInt("spanY", 1),
            packageName = optStringOrNull("packageName"),
            activityName = optStringOrNull("activityName"),
            label = optStringOrNull("label"),
            folderItemIds = folderIds,
            appWidgetId = json.optInt("appWidgetId", -1),
            widgetProviderPackage = optStringOrNull("widgetProviderPackage")
        )
    }

    /**
     * Records "app X was just opened" with a timestamp, used to drive
     * the dock's "recently used" ordering. Stored as a simple
     * packageName/activityName -> lastUsedMillis map; capped so it can't
     * grow unbounded over months of use.
     */
    fun recordAppUsage(packageName: String, activityName: String) {
        val key = "$packageName/$activityName"
        val map = loadUsageMap().toMutableMap()
        map[key] = System.currentTimeMillis()
        val trimmed = map.entries.sortedByDescending { it.value }.take(MAX_USAGE_ENTRIES)
        val json = JSONObject()
        trimmed.forEach { (k, v) -> json.put(k, v) }
        prefs.edit().putString(KEY_USAGE_MAP, json.toString()).apply()
    }

    /** Returns "packageName/activityName" keys ordered most-recently-used first. */
    fun loadRecentAppKeys(): List<String> =
        loadUsageMap().entries.sortedByDescending { it.value }.map { it.key }

    private fun loadUsageMap(): Map<String, Long> {
        val raw = prefs.getString(KEY_USAGE_MAP, null) ?: return emptyMap()
        return try {
            val json = JSONObject(raw)
            json.keys().asSequence().associateWith { json.getLong(it) }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    companion object {
        private const val PREFS_NAME = "omr_launcher_layout"
        private const val KEY_HOME_ITEMS = "home_items"
        private const val KEY_DOCK_ITEMS = "dock_items"
        private const val KEY_PAGE_COUNT = "page_count"
        private const val KEY_USAGE_MAP = "usage_map"
        private const val MAX_USAGE_ENTRIES = 50
        private const val SCHEMA_VERSION = 1
    }
}
