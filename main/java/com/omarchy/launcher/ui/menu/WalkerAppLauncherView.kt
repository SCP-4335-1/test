package com.omarchy.launcher.ui.menu

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.omarchy.launcher.R
import com.omarchy.launcher.data.AppInfo

/**
 * A dedicated, standalone app launcher modeled on omarchy-launch-walker:
 * that script starts the `elephant` data daemon plus the `walker`
 * --gapplication-service if not already running, then opens
 * `walker --width 644 --maxheight 300 --minheight 300` as a single-purpose
 * fuzzy-search box. There's no Android equivalent of those background
 * services to manage (the app list is just read straight from
 * LauncherApps each time this opens), but the *interaction model* is
 * copied directly: one search box, fuzzy-ranked results below it,
 * Enter or a tap launches the top/selected match immediately and closes
 * the box -- unlike the Omarchy-menu's "Apps" submenu, this is not a
 * fixed list of rows to browse, it's built to be typed into.
 */
class WalkerAppLauncherView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private lateinit var searchInput: android.widget.EditText
    private lateinit var resultsRecycler: androidx.recyclerview.widget.RecyclerView
    private lateinit var emptyState: android.widget.TextView

    private val adapter = WalkerAppAdapter { app -> launchAndClose(app) }

    private var allApps: List<AppInfo> = emptyList()
    private var rankedResults: List<AppInfo> = emptyList()

    var onLaunchApp: ((AppInfo) -> Unit)? = null
    var onRequestClose: (() -> Unit)? = null

    init {
        View.inflate(context, R.layout.view_walker_app_launcher, this)
        bindViews()
        setOnClickListener { /* consume: prevents tap-through to the closing overlay scrim */ }
        resultsRecycler.layoutManager = LinearLayoutManager(context)
        resultsRecycler.adapter = adapter

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                applyFuzzyFilter(s?.toString().orEmpty())
            }
        })

        // Enter/Go on the keyboard launches the top-ranked match, exactly
        // like pressing Enter in walker launches its first/highlighted result.
        searchInput.setOnEditorActionListener { _, actionId, event ->
            val isGo = actionId == EditorInfo.IME_ACTION_GO
            val isEnterKeyDown = event != null &&
                event.keyCode == KeyEvent.KEYCODE_ENTER &&
                event.action == KeyEvent.ACTION_DOWN
            if (isGo || isEnterKeyDown) {
                rankedResults.firstOrNull()?.let { launchAndClose(it) }
                true
            } else {
                false
            }
        }
    }

    private fun bindViews() {
        searchInput = findViewById(R.id.walkerSearchInput)
        resultsRecycler = findViewById(R.id.walkerResultsRecycler)
        emptyState = findViewById(R.id.walkerEmptyState)
    }

    /** Call every time the launcher opens fresh -- the app list may have changed since last time. */
    fun open(apps: List<AppInfo>) {
        allApps = apps
        searchInput.setText("")
        applyFuzzyFilter("")
        requestSearchFocus()
    }

    fun requestSearchFocus() {
        searchInput.requestFocus()
    }

    private fun launchAndClose(app: AppInfo) {
        onLaunchApp?.invoke(app)
        onRequestClose?.invoke()
    }

    /**
     * Lightweight fuzzy match: ranks apps whose label either contains the
     * query as a substring (highest priority, ordered by match position)
     * or contains every query character in order as a subsequence (e.g.
     * "ss" matches "Settings"), which covers the common walker/elephant
     * fuzzy-matching feel without needing a real scoring library.
     */
    private fun applyFuzzyFilter(query: String) {
        rankedResults = if (query.isBlank()) {
            allApps
        } else {
            val q = query.lowercase()
            allApps
                .mapNotNull { app ->
                    val label = app.label.lowercase()
                    val containsIndex = label.indexOf(q)
                    when {
                        containsIndex >= 0 -> app to containsIndex
                        isSubsequence(q, label) -> app to (label.length + q.length)
                        else -> null
                    }
                }
                .sortedBy { it.second }
                .map { it.first }
        }

        adapter.submitList(rankedResults)
        val isEmpty = rankedResults.isEmpty()
        emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        resultsRecycler.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun isSubsequence(query: String, text: String): Boolean {
        var qi = 0
        for (c in text) {
            if (qi < query.length && c == query[qi]) qi++
            if (qi == query.length) return true
        }
        return query.isEmpty()
    }
}
