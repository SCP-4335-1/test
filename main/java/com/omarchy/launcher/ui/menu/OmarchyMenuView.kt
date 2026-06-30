package com.omarchy.launcher.ui.menu

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import com.omarchy.launcher.R

/**
 * The actual wofi/walker-style popup: a search box at the top, a
 * vertical list of the current screen's entries below, and a
 * breadcrumb-driven back stack so selecting a Submenu drills in while
 * back/empty-search pops back out -- exactly how the real
 * omarchy-menu's back_to()/show_*_menu() functions chain together,
 * just modeled as a navigation stack instead of recursive bash
 * function calls.
 */
class OmarchyMenuView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private lateinit var titleText: android.widget.TextView
    private lateinit var searchInput: android.widget.EditText
    private lateinit var rowsRecycler: androidx.recyclerview.widget.RecyclerView
    private lateinit var backRow: View
    private lateinit var backLabel: android.widget.TextView

    private val adapter = OmarchyMenuAdapter { entry -> onEntrySelected(entry) }

    // Navigation stack: each frame is (title, full entry list for that screen)
    private data class MenuFrame(val title: String, val entries: List<OmarchyMenuEntry>)
    private val backStack = mutableListOf<MenuFrame>()

    var onRequestClose: (() -> Unit)? = null
    var onRequestOpenApps: (() -> Unit)? = null

    init {
        View.inflate(context, R.layout.view_omarchy_menu, this)
        bindViews()
        setOnClickListener { /* consume: prevents tap-through to the closing overlay scrim */ }
        rowsRecycler.layoutManager = LinearLayoutManager(context)
        rowsRecycler.adapter = adapter

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                applyFilter(s?.toString().orEmpty())
            }
        })

        backRow.setOnClickListener { goBack() }
    }

    private fun bindViews() {
        titleText = findViewById(R.id.omarchyMenuTitle)
        searchInput = findViewById(R.id.omarchyMenuSearch)
        rowsRecycler = findViewById(R.id.omarchyMenuRows)
        backRow = findViewById(R.id.omarchyMenuBackRow)
        backLabel = findViewById(R.id.omarchyMenuBackLabel)
    }

    /** Call every time the menu is opened fresh, with a newly-built tree (app list may have changed). */
    fun open(rootTitle: String, rootEntries: List<OmarchyMenuEntry>) {
        backStack.clear()
        backStack.add(MenuFrame(rootTitle, rootEntries))
        searchInput.setText("")
        renderCurrentFrame()
    }

    private fun onEntrySelected(entry: OmarchyMenuEntry) {
        when (entry) {
            is OmarchyMenuEntry.Action -> entry.onSelect()
            is OmarchyMenuEntry.Submenu -> {
                backStack.add(MenuFrame(entry.label, entry.children))
                searchInput.setText("")
                renderCurrentFrame()
            }
        }
    }

    private fun goBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
            searchInput.setText("")
            renderCurrentFrame()
        } else {
            onRequestClose?.invoke()
        }
    }

    /** Hardware/gesture back button support -- call from the hosting Activity's onBackPressed. */
    fun handleBackPressed(): Boolean {
        goBack()
        return true
    }

    private fun renderCurrentFrame() {
        val frame = backStack.last()
        titleText.text = frame.title
        backRow.visibility = if (backStack.size > 1) View.VISIBLE else View.GONE
        backLabel.text = context.getString(R.string.omarchy_menu_back)
        applyFilter(searchInput.text?.toString().orEmpty())
        rowsRecycler.scrollToPosition(0)
    }

    private fun applyFilter(query: String) {
        val frame = backStack.last()
        val filtered = if (query.isBlank()) {
            frame.entries
        } else {
            frame.entries.filter { it.label.contains(query, ignoreCase = true) }
        }
        adapter.submitList(filtered)
    }
}
