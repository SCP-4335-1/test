package com.omarchy.launcher.ui.home

import android.app.WallpaperManager
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.PopupWindow
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.viewpager2.widget.ViewPager2
import com.omarchy.launcher.LauncherApplication
import com.omarchy.launcher.R
import com.omarchy.launcher.data.AppInfo
import com.omarchy.launcher.databinding.ActivityHomeBinding
import com.omarchy.launcher.gesture.LauncherGestureDetector
import com.omarchy.launcher.ui.SettingsActivity
import com.omarchy.launcher.ui.menu.OmarchyMenuBuilder
import com.omarchy.launcher.util.GlitchEffectHelper
import com.omarchy.launcher.ui.widget.WidgetHostManager

private const val GRID_COLUMNS = 5
private const val GRID_ROWS = 6
private const val DOCK_DEFAULT_SIZE = 5
private const val SCRIM_ARM_DELAY_MS = 250L
private var baseDockHeight = 0
private leteint var widgetHostManager: WidgetHostManager

/**
 * The actual HOME activity. Registered with category.HOME in the
 * manifest, launchMode singleTask so re-pressing Home never stacks a
 * second instance on top of itself.
 *
 * Responsibilities deliberately centralized here rather than split into
 * fragments: a launcher's home screen has exactly one instance for the
 * lifetime of the foreground session, so fragment lifecycle overhead
 * buys nothing -- plain view-binding + a shared ViewModel is enough.
 */
class HomeActivity : AppCompatActivity(), LauncherGestureDetector.Callback {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var gestureDetector: LauncherGestureDetector

    private val viewModel: HomeViewModel by viewModels {
        val app = LauncherApplication.from(this)
        HomeViewModel.Factory(app.appRepository, app.layoutStore)
    }

    private var drawerOpen = false
    private var omarchyMenuOpen = false
    private var omarchyMenuScrimArmed = false
    private var walkerScrimArmed = false
    private var walkerLauncherOpen = false
    private var pagerAdapter: HomePagerAdapter? = null
    // Once the user picks a preset/gallery wallpaper this session, onResume
    // must stop re-reading WallpaperManager: setBitmap()'s write isn't
    // guaranteed to be visible to an immediate read-back (OEM caching), so
    // re-querying on the very next onResume (e.g. returning from the
    // gallery picker) was clobbering the freshly-applied bitmap with the
    // stale system wallpaper before it had a chance to be seen.
    private var customWallpaperAppliedThisSession = false

    private val widgetPickerLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val appWidgetId = result.data?.getIntExtra(
                android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, -1
            ) ?: -1
            if (appWidgetId != -1) {

                val info = widgetHostManager.getProviderInfo(appWidgetId)

                if (info != null) {

                    val hostView =
                    widgetHostManager.createHostView(appWidgetId, info)

                    hostView.setAppWidget(appWidgetId, info)

                    binding.homeGrid.addView(hostView)
                }
            }
        }
    }

    private val galleryImagePickerLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data
        if (result.resultCode == RESULT_OK && uri != null) {
            val bitmap = com.omarchy.launcher.ui.wallpaper.WallpaperPresetRenderer.applyFromUri(this, uri)
            if (bitmap != null) {
                customWallpaperAppliedThisSession = true
                binding.wallpaperView.setImageBitmap(bitmap)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        widgetHostManager = WidgetHostManager(this)

        gestureDetector = LauncherGestureDetector(this, this)
        hideSystemStatusBar()

        setupWallpaper()
        setupGestureCapture()
        setupDrawer()
        setupOmarchyMenu()
        setupWalkerLauncher()
        observeViewModel()
    }

    // ---- Wallpaper -----------------------------------------------------

    /**
     * Hides the system status bar so our own StatusLineView is the only
     * top bar visible. Uses WindowInsetsController on API 30+ (this
     * device's target, Android 15, supports it) with the legacy
     * SYSTEM_UI flags as a fallback for any lower API path. Swipe-down
     * from the very top edge still reveals the system bar temporarily
     * (standard Android behavior) -- there is no public API to block
     * that entirely from a regular app.
     */
    private fun hideSystemStatusBar() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)
                window.insetsController?.let { controller ->
                    controller.hide(android.view.WindowInsets.Type.statusBars())
                    controller.systemBarsBehavior =
                        android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                    android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                    )
            }
        } catch (e: Exception) {
            // Some OEM skins restrict status-bar hiding for non-system
            // apps; falling back to the system bar being visible is
            // harmless, so just swallow the failure here.
        }
        applyNavBarInsetToDock()
    }

    /**
     * setDecorFitsSystemWindows(false) stops the system from
     * auto-padding our layout away from the 3-button/gesture nav bar,
     * so the dock would otherwise render underneath it on devices like
     * the Galaxy A14 (3-button nav by default). Reading the bottom
     * inset directly and applying it as padding keeps the dock fully
     * visible above the nav bar regardless of nav mode.
     */
    /**
     * setDecorFitsSystemWindows(false) stops the system from
     * auto-padding our layout away from the 3-button/gesture nav bar,
     * so the dock would otherwise render underneath it on devices like
     * the Galaxy A14 (3-button nav by default). Reading the bottom
     * inset directly and applying it as padding keeps the dock fully
     * visible above the nav bar regardless of nav mode.
     *
     * The padding has to come out of *extra* height, not the dock's
     * existing fixed 76dp -- adding bottom padding inside that fixed
     * height was squeezing the icon row down into whatever was left
     * (as little as ~28dp on a 3-button-nav device), which is why the
     * dock icons here ended up rendering noticeably smaller/misplaced
     * compared to the variant that never applies this inset at all.
     * Growing the container by the inset amount first, then padding by
     * that same amount, keeps the icon row itself at the full 76dp --
     * identical in size/position to the other build -- with the inset
     * only adding empty space below it.
     */
    private fun applyNavBarInsetToDock() {
        val baseDockHeight = binding.dockContainer.layoutParams.height
        binding.rootHome.setOnApplyWindowInsetsListener { _, insets ->
            val navBarBottom = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                insets.getInsets(android.view.WindowInsets.Type.navigationBars()).bottom
            } else {
                @Suppress("DEPRECATION")
                insets.systemWindowInsetBottom
            }
            binding.dockContainer.layoutParams = binding.dockContainer.layoutParams.apply {
                height = baseDockHeight + navBarBottom
            }
            binding.dockContainer.setPadding(
                binding.dockContainer.paddingLeft,
                binding.dockContainer.paddingTop,
                binding.dockContainer.paddingRight,
                navBarBottom
            )
            insets
        }
        binding.rootHome.requestApplyInsets()
    }

    private fun setupWallpaper() {
        if (customWallpaperAppliedThisSession) return
        try {
            val wallpaperManager = WallpaperManager.getInstance(this)
            val drawable: Drawable? = try {
                wallpaperManager.getDrawable()
            } catch (e: Exception) {
                wallpaperManager.drawable
            }
            binding.wallpaperView.setImageDrawable(null)
            binding.wallpaperView.setImageDrawable(drawable)
        } catch (e: Exception) {
            // No wallpaper permission / no wallpaper set -- the void-black
            // theme background already looks intentional on its own.
        }
    }

    // ---- Gesture capture -------------------------------------------------
    // Using dispatchTouchEvent at the Activity level instead of a touch
    // listener on the root view: a listener on rootHome only sees events
    // the ViewPager2/HomeGridLayout children haven't already consumed,
    // which is exactly what was breaking swipe-up before (the pager's own
    // vertical/horizontal touch handling ate the gesture first).
    // dispatchTouchEvent always sees every event before any child does.

    private fun setupGestureCapture() {
        // no-op now; gesture feed happens in dispatchTouchEvent override below
    }

    override fun dispatchTouchEvent(event: android.view.MotionEvent): Boolean {
        if (!walkerLauncherOpen && !omarchyMenuOpen && !isTouchInsideDock(event) && !isTouchInsideOpenDrawer(event)) {
            gestureDetector.onTouchEvent(event)
        }
        return super.dispatchTouchEvent(event)
    }

    /**
     * While the app drawer is open, scrolling/flinging its app list was
     * also being fed into the homescreen-level swipe detector (the same
     * one that opens/closes the drawer from the bare homescreen). A
     * scroll-to-the-end fling inside the list has the same raw up/down
     * motion shape as a homescreen swipe, so it could trip onSwipeDown
     * and close the drawer out from under an ordinary scroll gesture --
     * even when the list was already at the bottom and visually doing
     * nothing but a harmless overscroll bounce. The drawer's own
     * RecyclerViews already handle their own scrolling, the explicit
     * close button, and back-press; the homescreen swipe detector has no
     * business seeing touches that land inside the open drawer at all.
     */
    private fun isTouchInsideOpenDrawer(event: android.view.MotionEvent): Boolean {
        if (!drawerOpen) return false
        val loc = IntArray(2)
        binding.drawerContainer.getLocationOnScreen(loc)
        val left = loc[0]
        val top = loc[1]
        val right = left + binding.drawerContainer.width
        val bottom = top + binding.drawerContainer.height
        return event.rawX >= left && event.rawX <= right && event.rawY >= top && event.rawY <= bottom
    }

    private fun isTouchInsideDock(event: android.view.MotionEvent): Boolean {
        val dockLoc = IntArray(2)
        binding.dockContainer.getLocationOnScreen(dockLoc)
        val pullTabLoc = IntArray(2)
        binding.drawerPullTab.getLocationOnScreen(pullTabLoc)
        val top = pullTabLoc[1]
        val bottom = dockLoc[1] + binding.dockContainer.height
        return event.rawY >= top && event.rawY <= bottom
    }

    override fun onSwipeUp() {
        if (!drawerOpen && !omarchyMenuOpen && !walkerLauncherOpen) openDrawer()
    }

    override fun onSwipeDown() {
        if (drawerOpen) closeDrawer()
    }

    override fun onDoubleTap() {
        if (!drawerOpen && !omarchyMenuOpen && !walkerLauncherOpen) {
            openOmarchyMenu()
        }
    }

    override fun onLongPress(x: Float, y: Float) {
        if (!drawerOpen) showHomeContextMenu(x, y)
    }

    override fun onSingleTapConfirmed() {
        if (drawerOpen) closeDrawer()
    }

    // ---- Drawer ----------------------------------------------------------

    private fun setupDrawer() {
        binding.appDrawerView.onAppClick = { app ->
            GlitchEffectHelper.playTapGlitch(binding.appDrawerView)
            viewModel.launchApp(app)
            closeDrawer()
        }
        binding.appDrawerView.onAppLongClick = { app, anchor ->
            showAppContextMenu(app, anchor, allowRemoveFromHome = false)
        }
        binding.appDrawerView.onWebSearch = { query ->
            performWebSearch(query)
            closeDrawer()
        }

        // Primary, always-visible affordance to open the drawer -- the
        // swipe-up gesture still works, but a button you can actually
        // see and tap is what makes the drawer discoverable at all.
        binding.drawerOpenButton.setOnClickListener {
            if (drawerOpen) closeDrawer() else openDrawer()
        }
        binding.drawerPullTab.setOnClickListener {
            if (drawerOpen) closeDrawer() else openDrawer()
        }
    }

    private fun openDrawer() {
        drawerOpen = true
        binding.drawerContainer.visibility = View.VISIBLE
        binding.appDrawerView.resetSearch()
        GlitchEffectHelper.playDrawerOpenGlitch(binding.drawerContainer)
    }

    private fun closeDrawer() {
        drawerOpen = false
        binding.drawerContainer.visibility = View.INVISIBLE
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Re-pressing Home while already in the launcher (singleTask
        // brings this same instance back) should always land on the
        // plain homescreen -- close every overlay rather than leaving
        // the drawer/menu sitting open underneath.
        if (walkerLauncherOpen) closeWalkerLauncher()
        if (omarchyMenuOpen) closeOmarchyMenu()
        if (drawerOpen) closeDrawer()
    }

    override fun onBackPressed() {
        if (walkerLauncherOpen) {
            closeWalkerLauncher()
        } else if (omarchyMenuOpen) {
            binding.omarchyMenuView.handleBackPressed()
        } else if (drawerOpen) {
            closeDrawer()
        } else {
            // Home is the base of the task stack -- there is intentionally
            // nothing "back" to navigate to, matching real launcher behavior.
            moveTaskToBack(true)
        }
    }

    // ---- Omarchy menu (wofi/walker-style, double-tap to open) -------------

    private fun setupOmarchyMenu() {
        binding.omarchyMenuView.onRequestClose = { closeOmarchyMenu() }

        // Tapping the scrim outside the centered popup closes the menu,
        // same as clicking outside a wofi/walker window.
        binding.omarchyMenuOverlay.setOnClickListener {
            if (omarchyMenuScrimArmed) closeOmarchyMenu()
        }
    }

    private fun openOmarchyMenu() {
        val builder = OmarchyMenuBuilder(
            context = this,
            onOpenWalkerLauncher = { openWalkerLauncher() },
            onOpenSettings = { startActivity(Intent(this, SettingsActivity::class.java)) },
            onOpenWallpaperPicker = { openWallpaperPicker() },
            onOpenWidgetPicker = { openWidgetPicker() },
            onCloseMenu = { closeOmarchyMenu() }
        )

        omarchyMenuOpen = true
        omarchyMenuScrimArmed = false
        binding.omarchyMenuOverlay.visibility = View.VISIBLE
        binding.omarchyMenuOverlay.postDelayed({ omarchyMenuScrimArmed = true }, SCRIM_ARM_DELAY_MS)
        binding.omarchyMenuView.open(
            getString(R.string.omarchy_menu_title_main),
            builder.buildMainMenu()
        )
        GlitchEffectHelper.playDrawerOpenGlitch(binding.omarchyMenuView)
    }

    private fun closeOmarchyMenu() {
        omarchyMenuOpen = false
        omarchyMenuScrimArmed = false
        binding.omarchyMenuOverlay.visibility = View.GONE
    }

    // ---- Walker-style app launcher (Omarchy menu's "Apps" entry) ----------

    private fun setupWalkerLauncher() {
        binding.walkerLauncherView.onLaunchApp = { app -> viewModel.launchApp(app) }
        binding.walkerLauncherView.onRequestClose = { closeWalkerLauncher() }

        // Tapping the scrim outside the centered box closes it, same as
        // clicking outside walker's own window.
        binding.walkerLauncherOverlay.setOnClickListener {
            if (walkerScrimArmed) closeWalkerLauncher()
        }
    }

    private fun openWalkerLauncher() {
        walkerLauncherOpen = true
        walkerScrimArmed = false
        binding.walkerLauncherOverlay.visibility = View.VISIBLE
        binding.walkerLauncherOverlay.postDelayed({ walkerScrimArmed = true }, SCRIM_ARM_DELAY_MS)
        binding.walkerLauncherView.open(viewModel.allApps.value.orEmpty())
        GlitchEffectHelper.playDrawerOpenGlitch(binding.walkerLauncherView)
    }

    private fun closeWalkerLauncher() {
        walkerLauncherOpen = false
        walkerScrimArmed = false
        binding.walkerLauncherOverlay.visibility = View.GONE
    }

    // ---- ViewModel / pager wiring -----------------------------------------

    private fun observeViewModel() {
        viewModel.allApps.observe(this, Observer { apps ->
            binding.appDrawerView.submitApps(apps)
            updateDockFromTopApps(apps)
        })

        viewModel.pageCount.observe(this, Observer { count ->
            setupPager(count)
        })

        viewModel.homeItems.observe(this, Observer {
            pagerAdapter?.notifyDataSetChanged()
        })
    }

    private fun setupPager(pageCount: Int) {
        pagerAdapter = HomePagerAdapter(
            pageCount = pageCount,
            columns = GRID_COLUMNS,
            rows = GRID_ROWS,
            itemsByPage = { page -> viewModel.itemsForPage(page) },
            iconResolver = { pkg, activity -> viewModel.resolveApp(pkg, activity) },
            onAppClick = { app ->
                viewModel.launchApp(app)
            },
            onAppLongClick = { app, anchor ->
                showAppContextMenu(app, anchor, allowRemoveFromHome = true)
            },
            onEmptyCellLongClick = { _, x, y, _ ->
                showHomeContextMenu(x.toFloat(), y.toFloat())
            }
        )
        binding.homePager.adapter = pagerAdapter
        binding.homePager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updatePageIndicator(position, pageCount)
            }
        })
        updatePageIndicator(binding.homePager.currentItem, pageCount)
    }

    private fun updatePageIndicator(current: Int, total: Int) {
        val sb = StringBuilder("[")
        for (i in 0 until total) {
            sb.append(if (i == current) "*" else "-")
        }
        sb.append("]")
        binding.pageIndicator.text = sb.toString()
    }

    private fun updateDockFromTopApps(apps: List<AppInfo>) {
        if (apps.isNotEmpty()) renderDock()
    }

    private fun renderDock() {
        val dockApps = viewModel.recentApps(DOCK_DEFAULT_SIZE)
        binding.dockView.setApps(dockApps)
        binding.dockView.onAppClickListener = { app -> viewModel.launchApp(app); renderDock() }
        binding.dockView.onAppLongClickListener = { app, anchor ->
            showAppContextMenu(app, anchor, allowRemoveFromHome = false)
        }
    }

    // ---- Context menus ----------------------------------------------------

    private fun showHomeContextMenu(x: Float, y: Float) {
        val popupView = layoutInflater.inflate(R.layout.popup_home_context_menu, binding.rootHome, false)
        val popup = PopupWindow(
            popupView,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        popupView.findViewById<android.widget.TextView>(R.id.menuWidgets).setOnClickListener {
            popup.dismiss()
            openWidgetPicker()
        }
        popupView.findViewById<android.widget.TextView>(R.id.menuWallpaper).setOnClickListener {
            popup.dismiss()
            openWallpaperPicker()
        }
        popupView.findViewById<android.widget.TextView>(R.id.menuSettings).setOnClickListener {
            popup.dismiss()
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        popup.showAtLocation(binding.rootHome, android.view.Gravity.NO_GRAVITY, x.toInt(), y.toInt())
    }

    private fun showAppContextMenu(app: AppInfo, anchor: View, allowRemoveFromHome: Boolean) {
        val popupView = layoutInflater.inflate(R.layout.popup_app_context_menu, binding.rootHome, false)
        val popup = PopupWindow(
            popupView,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        popupView.findViewById<android.widget.TextView>(R.id.menuAppLabel).text = app.label
        popupView.findViewById<android.widget.TextView>(R.id.menuPinHome).setOnClickListener {
            popup.dismiss()
            val (freeCol, freeRow) = findNextFreeCell(binding.homePager.currentItem)
            viewModel.addAppToHome(app, page = binding.homePager.currentItem, col = freeCol, row = freeRow)
            closeDrawer()
        }
        popupView.findViewById<android.widget.TextView>(R.id.menuAppInfo).setOnClickListener {
            popup.dismiss()
            viewModel.openAppInfo(app)
        }
        popupView.findViewById<android.widget.TextView>(R.id.menuUninstall).setOnClickListener {
            popup.dismiss()
            try {
                val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
                    data = android.net.Uri.parse("package:${app.packageName}")
                    putExtra(Intent.EXTRA_RETURN_RESULT, false)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } catch (e: Exception) {
                // System uninstall UI unavailable (rare OEM lockdown) --
                // fail quietly rather than crashing the whole launcher.
            }
        }
        val removeView = popupView.findViewById<android.widget.TextView>(R.id.menuRemove)
        if (allowRemoveFromHome) {
            removeView.visibility = View.VISIBLE
            removeView.setOnClickListener {
                popup.dismiss()
                // Item id lookup by package/activity on the current page;
                // good enough since duplicate placements of the same app
                // across the grid are rare and each occurrence removes
                // independently if tapped directly.
                viewModel.itemsForPage(binding.homePager.currentItem)
                    .firstOrNull { it.packageName == app.packageName && it.activityName == app.activityName }
                    ?.let { viewModel.removeHomeItem(it.id) }
            }
        }

        val location = IntArray(2)
        anchor.getLocationOnScreen(location)
        popup.showAtLocation(binding.rootHome, android.view.Gravity.NO_GRAVITY, location[0], location[1])
    }

    // ---- Widgets / wallpaper launchers ------------------------------------

    /**
     * Mirrors typing a non-app query into a walker/spotlight-style
     * launcher: ACTION_WEB_SEARCH hands the query straight to whatever
     * the user has set as their default search provider/browser, which
     * is the standard, most broadly-supported way to do this on Android
     * (works the same on stock Android and Samsung's OneUI skin).
     */
    private fun performWebSearch(query: String) {
        try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(android.app.SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback: open a search-engine URL directly in the browser
            // if no ACTION_WEB_SEARCH handler exists on this device.
            try {
                val fallback = Intent(Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse(
                        "https://www.google.com/search?q=${android.net.Uri.encode(query)}"
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(fallback)
            } catch (e2: Exception) {
                // No browser at all on this device -- nothing more we can do.
            }
        }
    }

    private fun openWidgetPicker() {
        widgetPickerLauncher.launch(
            Intent(this, com.omarchy.launcher.ui.widgets.WidgetPickerActivity::class.java)
        )
    }

    private fun openWallpaperPicker() {
        com.omarchy.launcher.ui.wallpaper.WallpaperPickerDialog.show(
            context = this,
            anchor = binding.rootHome,
            onPresetApplied = { bitmap ->
                customWallpaperAppliedThisSession = true
                binding.wallpaperView.setImageBitmap(bitmap)
            },
            onPickFromGallery = {
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "image/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                try {
                    galleryImagePickerLauncher.launch(intent)
                } catch (e: Exception) {
                    // No image picker available on this device.
                }
            }
        )
    }

    /**
     * Finds the first unoccupied cell on the given page, scanning
     * row-major (left-to-right, top-to-bottom). Falls back to (0,0) if
     * the page is completely full -- pinning then just overlaps the
     * first icon rather than throwing, since a full 5x6 grid (30 apps
     * pinned to one page) is already an edge case the user would notice
     * and fix themselves by adding a page.
     */
    private fun findNextFreeCell(page: Int): Pair<Int, Int> {
        val occupied = viewModel.itemsForPage(page).map { it.col to it.row }.toSet()
        for (row in 0 until GRID_ROWS) {
            for (col in 0 until GRID_COLUMNS) {
                if ((col to row) !in occupied) return col to row
            }
        }
        return 0 to 0
    }

    override fun onResume() {
        super.onResume()
        hideSystemStatusBar()
        setupWallpaper()
        renderDock()
        widgetHostManager.startListening()
        binding.statusLine.startUpdates()
    }

    override fun onPause() {
        widgetHostManager.stopListening()
        super.onPause()
        binding.statusLine.stopUpdates()
    }
}
