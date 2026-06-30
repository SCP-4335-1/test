package com.omarchy.launcher.ui.drawer

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.omarchy.launcher.R
import com.omarchy.launcher.data.AppInfo
import com.omarchy.launcher.util.GlitchEffectHelper

/**
 * Renders each app as an icon inside a neon-bordered frame. The
 * "glitch-hover" effect (a quick jitter+flicker) plays on ACTION_DOWN
 * rather than waiting for the click to complete, so it reads as
 * immediate tactile feedback rather than a click confirmation.
 */
class NeonGridAdapter(
    private val onClick: (AppInfo) -> Unit,
    private val onLongClick: (AppInfo, View) -> Unit
) : RecyclerView.Adapter<NeonGridAdapter.GridViewHolder>() {

    private val items = mutableListOf<AppInfo>()

    fun submitList(newItems: List<AppInfo>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class GridViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: android.widget.ImageView = view.findViewById(R.id.itemIcon)
        val label: android.widget.TextView = view.findViewById(R.id.itemLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_grid, parent, false)
        return GridViewHolder(view)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: GridViewHolder, position: Int) {
        val app = items[position]
        holder.icon.setImageDrawable(app.icon)
        holder.label.text = app.label

        holder.itemView.setOnTouchListener { v, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                GlitchEffectHelper.playTapGlitch(holder.icon)
            }
            false
        }
        holder.itemView.setOnClickListener { onClick(app) }
        holder.itemView.setOnLongClickListener { v ->
            onLongClick(app, v)
            true
        }
    }

    override fun getItemCount(): Int = items.size
}
