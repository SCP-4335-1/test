package com.omarchy.launcher.ui.home

import android.content.Context
import android.util.AttributeSet
import android.view.DragEvent
import android.view.View
import android.view.ViewGroup
import com.omarchy.launcher.R

/**
 * A simple, fixed-cell-count grid container for homescreen icons/widgets.
 * Unlike a GridLayout/RecyclerView, children here carry explicit
 * (col, row, spanX, spanY) layout params so we can freely place icons
 * anywhere on the grid (including leaving gaps), which is what Nova
 * Launcher's homescreen editing feels like.
 *
 * Drag-and-drop re-positioning is handled here via the platform
 * View.startDragAndDrop / OnDragListener APIs rather than manual touch
 * tracking, since DnD already solves cross-view drag targets (e.g.
 * dragging an icon onto another icon to form a folder) for free.
 */
class HomeGridLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewGroup(context, attrs) {

    var columns: Int = 5
    var rows: Int = 6

    var onCellTapListener: ((col: Int, row: Int) -> Unit)? = null
    var onItemDroppedListener: ((view: View, col: Int, row: Int) -> Unit)? = null

    private val dragListener = OnDragListener { _, event ->
        when (event.action) {
            DragEvent.ACTION_DROP -> {
                val (col, row) = cellAt(event.x, event.y)
                val draggedView = event.localState as? View
                if (draggedView != null) {
                    onItemDroppedListener?.invoke(draggedView, col, row)
                }
                true
            }
            DragEvent.ACTION_DRAG_STARTED -> true
            else -> true
        }
    }

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.HomeGridLayout, 0, 0).apply {
            try {
                columns = getInteger(R.styleable.HomeGridLayout_columns, 5)
                rows = getInteger(R.styleable.HomeGridLayout_rows, 6)
            } finally {
                recycle()
            }
        }

        setOnDragListener(dragListener)
    }

    class LayoutParams(
        var col: Int,
        var row: Int,
        var spanX: Int = 1,
        var spanY: Int = 1
    ) : ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val cellWidth = width / columns
        val cellHeight = height / rows

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val lp = child.layoutParams as? LayoutParams ?: continue
            val childWidth = cellWidth * lp.spanX
            val childHeight = cellHeight * lp.spanY
            child.measure(
                MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.AT_MOST)
            )
        }
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val cellWidth = (r - l) / columns
        val cellHeight = (b - t) / rows

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val lp = child.layoutParams as? LayoutParams ?: continue

            val cellLeft = lp.col * cellWidth
            val cellTop = lp.row * cellHeight
            val cellRight = cellLeft + cellWidth * lp.spanX
            val cellBottom = cellTop + cellHeight * lp.spanY

            // center child within its cell span since measured size may be
            // smaller than the cell (icons don't stretch to fill cells)
            val childW = child.measuredWidth
            val childH = child.measuredHeight
            val offsetX = ((cellRight - cellLeft) - childW) / 2
            val offsetY = ((cellBottom - cellTop) - childH) / 2

            child.layout(
                cellLeft + offsetX,
                cellTop + offsetY,
                cellLeft + offsetX + childW,
                cellTop + offsetY + childH
            )
        }
    }

    fun addItem(view: View, col: Int, row: Int, spanX: Int = 1, spanY: Int = 1) {
        view.layoutParams = LayoutParams(col, row, spanX, spanY)
        addView(view)
    }

    /** Returns true if the requested span is fully unoccupied at the given origin. */
    fun isCellRangeFree(col: Int, row: Int, spanX: Int, spanY: Int, ignoring: View? = null): Boolean {
        if (col < 0 || row < 0 || col + spanX > columns || row + spanY > rows) return false
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child === ignoring) continue
            val lp = child.layoutParams as? LayoutParams ?: continue
            val overlapsX = col < lp.col + lp.spanX && col + spanX > lp.col
            val overlapsY = row < lp.row + lp.spanY && row + spanY > lp.row
            if (overlapsX && overlapsY) return false
        }
        return true
    }

    fun cellAt(x: Float, y: Float): Pair<Int, Int> {
        val cellWidth = width / columns
        val cellHeight = height / rows
        val col = (x / cellWidth).toInt().coerceIn(0, columns - 1)
        val row = (y / cellHeight).toInt().coerceIn(0, rows - 1)
        return col to row
    }
}
