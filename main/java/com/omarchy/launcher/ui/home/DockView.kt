package com.omarchy.launcher.ui.home

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.omarchy.launcher.R
import com.omarchy.launcher.data.AppInfo
import com.omarchy.launcher.util.GlitchEffectHelper

/**
 * Dock row: a small fixed set of favorite apps always visible at the
 * bottom of the homescreen, independent of which page is currently
 * showing in the ViewPager2 above it. Plain LinearLayout of icon items
 * since the dock never needs more than ~5-6 slots and doesn't need
 * RecyclerView's recycling machinery.
 */
class DockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    var onAppClickListener: ((AppInfo) -> Unit)? = null
    var onAppLongClickListener: ((AppInfo, android.view.View) -> Unit)? = null

    init {
        orientation = HORIZONTAL
    }

    fun setApps(apps: List<AppInfo>) {
        removeAllViews()
        val inflater = LayoutInflater.from(context)
        apps.forEach { app ->
            val itemView = inflater.inflate(R.layout.item_dock_icon, this, false)
            val icon = itemView.findViewById<android.widget.ImageView>(R.id.itemIcon)
            icon.setImageDrawable(app.icon)
            itemView.setOnClickListener {
                GlitchEffectHelper.playTapGlitch(icon)
                onAppClickListener?.invoke(app)
            }
            itemView.setOnLongClickListener {
                onAppLongClickListener?.invoke(app, itemView)
                true
            }
            addView(itemView)
        }
    }
}
