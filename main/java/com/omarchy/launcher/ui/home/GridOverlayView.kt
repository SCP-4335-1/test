package com.omarchy.launcher.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.omarchy.launcher.R

/**
 * Draws a very faint grid of vertical/horizontal lines across the
 * homescreen, evoking Hyprland/i3-style tiling WM borders. Pure Canvas
 * drawing -- this is intentionally cheap (a handful of drawLine calls)
 * since it repaints on every homescreen frame the user might scroll
 * through.
 */
class GridOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val linePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.grid_line)
        strokeWidth = 1f
        style = Paint.Style.STROKE
        isAntiAlias = false
    }

    /** How many cells to draw across/down -- purely decorative, not tied to the real app grid. */
    var cellsX: Int = 5
        set(value) { field = value; invalidate() }

    var cellsY: Int = 6
        set(value) { field = value; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        val cellWidth = width.toFloat() / cellsX
        val cellHeight = height.toFloat() / cellsY

        for (i in 1 until cellsX) {
            val x = i * cellWidth
            canvas.drawLine(x, 0f, x, height.toFloat(), linePaint)
        }
        for (j in 1 until cellsY) {
            val y = j * cellHeight
            canvas.drawLine(0f, y, width.toFloat(), y, linePaint)
        }
    }
}
