package com.omarchy.launcher.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.omarchy.launcher.data.AppInfo
import com.omarchy.launcher.data.AppRepository
import com.omarchy.launcher.data.HomeItem
import com.omarchy.launcher.data.HomeItemType
import com.omarchy.launcher.data.LayoutStore
import kotlinx.coroutines.launch

/**
 * Owns the in-memory state HomeActivity renders: the full app list (for
 * the drawer/search), and the placed home/dock items (for the grid
 * pages). Survives configuration changes so rotating the device or a
 * theme change doesn't force a full app-list reload from LauncherApps.
 */
class HomeViewModel(
    private val appRepository: AppRepository,
    private val layoutStore: LayoutStore
) : ViewModel() {

    private val _allApps = MutableLiveData<List<AppInfo>>(emptyList())
    val allApps: LiveData<List<AppInfo>> = _allApps

    private val _homeItems = MutableLiveData<List<HomeItem>>(emptyList())
    val homeItems: LiveData<List<HomeItem>> = _homeItems

    private val _dockItems = MutableLiveData<List<HomeItem>>(emptyList())
    val dockItems: LiveData<List<HomeItem>> = _dockItems

    private val _pageCount = MutableLiveData(1)
    val pageCount: LiveData<Int> = _pageCount

    private var appsByKey: Map<String, AppInfo> = emptyMap()

    init {
        loadApps()
        _homeItems.value = layoutStore.loadHomeItems()
        _dockItems.value = layoutStore.loadDockItems()
        _pageCount.value = layoutStore.loadPageCount().coerceAtLeast(1)

        appRepository.addOnChangedListener { loadApps() }
    }

    private fun loadApps() {
        viewModelScope.launch {
            val apps = appRepository.loadAllApps()
            appsByKey = apps.associateBy { "${it.packageName}/${it.activityName}" }
            _allApps.value = apps
        }
    }

    fun resolveApp(packageName: String, activityName: String): AppInfo? =
        appsByKey["$packageName/$activityName"]

    fun itemsForPage(page: Int): List<HomeItem> =
        _homeItems.value.orEmpty().filter { it.page == page }

    fun launchApp(app: AppInfo) {
        appRepository.launch(app)
        layoutStore.recordAppUsage(app.packageName, app.activityName)
    }

    /** Most-recently-launched apps first, falling back to alphabetical order for apps never opened yet. */
    fun recentApps(limit: Int): List<AppInfo> {
        val recentKeys = layoutStore.loadRecentAppKeys()
        val byKey = appsByKey
        val used = recentKeys.mapNotNull { byKey[it] }
        if (used.size >= limit) return used.take(limit)
        val remainder = byKey.values.filterNot { app ->
            used.any { it.packageName == app.packageName && it.activityName == app.activityName }
        }
        return (used + remainder).take(limit)
    }

    fun openAppInfo(app: AppInfo) {
        appRepository.openAppInfo(app)
    }

    fun addAppToHome(app: AppInfo, page: Int, col: Int, row: Int) {
        val newItem = HomeItem(
            id = "app_${app.packageName}_${System.currentTimeMillis()}",
            type = HomeItemType.APP,
            page = page,
            col = col,
            row = row,
            packageName = app.packageName,
            activityName = app.activityName,
            label = app.label
        )
        val updated = _homeItems.value.orEmpty() + newItem
        _homeItems.value = updated
        layoutStore.saveHomeItems(updated)
    }

    fun removeHomeItem(itemId: String) {
        val updated = _homeItems.value.orEmpty().filterNot { it.id == itemId }
        _homeItems.value = updated
        layoutStore.saveHomeItems(updated)
    }

    fun moveHomeItem(itemId: String, newPage: Int, newCol: Int, newRow: Int) {
        val updated = _homeItems.value.orEmpty().map {
            if (it.id == itemId) it.copy(page = newPage, col = newCol, row = newRow) else it
        }
        _homeItems.value = updated
        layoutStore.saveHomeItems(updated)
    }

    fun setDockApps(apps: List<AppInfo>) {
        val items = apps.mapIndexed { index, app ->
            HomeItem(
                id = "dock_${app.packageName}_$index",
                type = HomeItemType.APP,
                page = 0,
                col = index,
                row = 0,
                packageName = app.packageName,
                activityName = app.activityName,
                label = app.label
            )
        }
        _dockItems.value = items
        layoutStore.saveDockItems(items)
    }

    fun addPage() {
        val newCount = (_pageCount.value ?: 1) + 1
        _pageCount.value = newCount
        layoutStore.savePageCount(newCount)
    }

    fun exportLayout(): String = layoutStore.exportLayout()

    fun importLayout(serialized: String): Boolean {
        val success = layoutStore.importLayout(serialized)
        if (success) {
            _homeItems.value = layoutStore.loadHomeItems()
            _dockItems.value = layoutStore.loadDockItems()
            _pageCount.value = layoutStore.loadPageCount().coerceAtLeast(1)
        }
        return success
    }

    class Factory(
        private val appRepository: AppRepository,
        private val layoutStore: LayoutStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(appRepository, layoutStore) as T
        }
    }
}
