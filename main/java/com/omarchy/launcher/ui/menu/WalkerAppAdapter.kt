package com.omarchy.launcher.ui.menu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.omarchy.launcher.R
import com.omarchy.launcher.data.AppInfo

/**
 * Renders ranked app matches for the Walker-style launcher. Deliberately
 * simpler than the drawer's adapters (icon + label only, no permission
 * bits or frame decoration) -- walker's own result rows are similarly
 * minimal, since the box is meant to be glanced at and dismissed in
 * under a second, not browsed.
 */
class WalkerAppAdapter(
    private val onSelect: (AppInfo) -> Unit
) : RecyclerView.Adapter<WalkerAppAdapter.RowViewHolder>() {

    private val items = mutableListOf<AppInfo>()

    fun submitList(newItems: List<AppInfo>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class RowViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: android.widget.ImageView = view.findViewById(R.id.walkerItemIcon)
        val label: android.widget.TextView = view.findViewById(R.id.walkerItemLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_walker_app_row, parent, false)
        return RowViewHolder(view)
    }

    override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
        val app = items[position]
        holder.icon.setImageDrawable(app.icon)
        holder.label.text = app.label
        holder.itemView.setOnClickListener { onSelect(app) }
    }

    override fun getItemCount(): Int = items.size
}
