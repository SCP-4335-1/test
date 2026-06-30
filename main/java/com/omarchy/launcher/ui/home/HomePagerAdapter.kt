package com.omarchy.launcher.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.omarchy.launcher.R
import com.omarchy.launcher.data.AppInfo
import com.omarchy.launcher.data.HomeItem

/**
 * Feeds ViewPager2 one HomeGridLayout per homescreen page. Implemented as
 * a plain RecyclerView.Adapter (which is what ViewPager2 wraps) since
 * each page is structurally identical -- only its set of placed items
 * differs.
 */
class HomePagerAdapter(
    private val pageCount: Int,
    private val columns: Int,
    private val rows: Int,
    private val itemsByPage: (page: Int) -> List<HomeItem>,
    private val iconResolver: (packageName: String, activityName: String) -> AppInfo?,
    private val onAppClick: (AppInfo) -> Unit,
    private val onAppLongClick: (AppInfo, View) -> Unit,
    private val onEmptyCellLongClick: (page: Int, col: Int, row: Int, anchor: View) -> Unit
) : RecyclerView.Adapter<HomePagerAdapter.PageViewHolder>() {

    inner class PageViewHolder(val gridLayout: HomeGridLayout) : RecyclerView.ViewHolder(gridLayout)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.page_home_grid, parent, false) as HomeGridLayout
        view.columns = columns
        view.rows = rows
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val grid = holder.gridLayout
        grid.removeAllViews()

        val items = itemsByPage(position)
        val inflater = LayoutInflater.from(grid.context)

        items.forEach { item ->
            val pkg = item.packageName
            val activity = item.activityName
            if (pkg != null && activity != null) {
                val app = iconResolver(pkg, activity) ?: return@forEach
                val itemView = inflater.inflate(R.layout.item_home_icon, grid, false)
                itemView.findViewById<android.widget.ImageView>(R.id.itemIcon).setImageDrawable(app.icon)
                itemView.findViewById<android.widget.TextView>(R.id.itemLabel).text = app.label
                itemView.setOnClickListener { onAppClick(app) }
                itemView.setOnLongClickListener { v ->
                    onAppLongClick(app, v)
                    true
                }
                grid.addItem(itemView, item.col, item.row, item.spanX, item.spanY)
            }
        }

        grid.setOnTouchListener { v, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP) {
                val (col, row) = grid.cellAt(event.x, event.y)
                if (grid.isCellRangeFree(col, row, 1, 1)) {
                    // empty cell tapped; HomeActivity decides what (if
                    // anything) happens via the gesture detector instead,
                    // this listener only matters for long-press-on-empty-cell
                }
            }
            false
        }
    }

    override fun getItemCount(): Int = pageCount
}
