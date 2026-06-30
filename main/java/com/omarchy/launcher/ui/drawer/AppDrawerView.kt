package com.omarchy.launcher.ui.drawer

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.omarchy.launcher.R
import com.omarchy.launcher.data.AppInfo

enum class DrawerMode { LIST, GRID }

/**
 * Wraps view_app_drawer.xml's contents (search box, mode toggle, both
 * RecyclerViews) and owns the filtering + mode-switching behavior, so
 * HomeActivity only has to hand it the full app list once and listen
 * for click callbacks -- it doesn't need to know list vs. grid exists
 * internally.
 */
class AppDrawerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private lateinit var searchInput: android.widget.EditText
    private lateinit var modeToggle: android.widget.ImageButton
    private lateinit var listRecycler: androidx.recyclerview.widget.RecyclerView
    private lateinit var gridRecycler: androidx.recyclerview.widget.RecyclerView
    private lateinit var emptyState: android.widget.TextView

    private val listAdapter = TerminalListAdapter(
        onClick = { onAppClick?.invoke(it) },
        onLongClick = { app, v -> onAppLongClick?.invoke(app, v) }
    )
    private val gridAdapter = NeonGridAdapter(
        onClick = { onAppClick?.invoke(it) },
        onLongClick = { app, v -> onAppLongClick?.invoke(app, v) }
    )

    private var allApps: List<AppInfo> = emptyList()
    private var lastFilteredResults: List<AppInfo> = emptyList()
    private var currentMode: DrawerMode = DrawerMode.GRID

    var onAppClick: ((AppInfo) -> Unit)? = null
    var onAppLongClick: ((AppInfo, View) -> Unit)? = null
    var onModeChanged: ((DrawerMode) -> Unit)? = null
    /** Invoked with the current query when the user wants to search the web instead of an app. */
    var onWebSearch: ((String) -> Unit)? = null

    init {
        View.inflate(context, R.layout.view_app_drawer, this)
        bindViews()
        listRecycler.layoutManager = LinearLayoutManager(context)
        listRecycler.adapter = listAdapter
        gridRecycler.layoutManager = GridLayoutManager(context, GRID_COLUMNS)
        gridRecycler.adapter = gridAdapter

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                applyFilter(s?.toString().orEmpty())
            }
        })

        // Pressing the keyboard's search/go action with no app match
        // falls back to a web search, like typing a non-app query into
        // walker/spotlight-style launchers.
        searchInput.setOnEditorActionListener { _, _, _ ->
            val query = searchInput.text?.toString().orEmpty()
            if (query.isNotBlank() && lastFilteredResults.isEmpty()) {
                onWebSearch?.invoke(query)
                true
            } else {
                false
            }
        }

        emptyState.setOnClickListener {
            val query = searchInput.text?.toString().orEmpty()
            if (query.isNotBlank()) onWebSearch?.invoke(query)
        }

        modeToggle.setOnClickListener { toggleMode() }
        setMode(DrawerMode.GRID, persistChoice = false)
    }

    private fun bindViews() {
        searchInput = findViewById(R.id.drawerSearchInput)
        modeToggle = findViewById(R.id.drawerModeToggle)
        listRecycler = findViewById(R.id.drawerListRecycler)
        gridRecycler = findViewById(R.id.drawerGridRecycler)
        emptyState = findViewById(R.id.drawerEmptyState)
    }

    fun submitApps(apps: List<AppInfo>) {
        allApps = apps
        applyFilter(searchInput.text?.toString().orEmpty())
    }

    fun setMode(mode: DrawerMode, persistChoice: Boolean = true) {
        currentMode = mode
        when (mode) {
            DrawerMode.LIST -> {
                listRecycler.visibility = View.VISIBLE
                gridRecycler.visibility = View.GONE
                modeToggle.setImageResource(R.drawable.ic_grid_mode)
            }
            DrawerMode.GRID -> {
                listRecycler.visibility = View.GONE
                gridRecycler.visibility = View.VISIBLE
                modeToggle.setImageResource(R.drawable.ic_list_mode)
            }
        }
        if (persistChoice) {
            onModeChanged?.invoke(mode)
        }
    }

    private fun toggleMode() {
        setMode(if (currentMode == DrawerMode.LIST) DrawerMode.GRID else DrawerMode.LIST)
    }

    /** Clears the search box and scrolls both lists back to top -- call when the drawer re-opens. */
    fun resetSearch() {
        searchInput.setText("")
        listRecycler.scrollToPosition(0)
        gridRecycler.scrollToPosition(0)
    }

    private fun applyFilter(query: String) {
        val filtered = if (query.isBlank()) {
            allApps
        } else {
            allApps.filter { it.label.contains(query, ignoreCase = true) }
        }
        lastFilteredResults = filtered
        listAdapter.submitList(filtered)
        gridAdapter.submitList(filtered)

        val isEmpty = filtered.isEmpty()
        if (isEmpty && query.isNotBlank()) {
            emptyState.text = context.getString(R.string.drawer_web_search_hint, query)
        } else {
            emptyState.text = context.getString(R.string.drawer_empty)
        }
        emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        if (isEmpty) {
            listRecycler.visibility = View.GONE
            gridRecycler.visibility = View.GONE
        } else {
            listRecycler.visibility = if (currentMode == DrawerMode.LIST) View.VISIBLE else View.GONE
            gridRecycler.visibility = if (currentMode == DrawerMode.GRID) View.VISIBLE else View.GONE
        }
    }

    companion object {
        private const val GRID_COLUMNS = 4
    }
}
