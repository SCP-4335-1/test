package com.omarchy.launcher.ui.menu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.omarchy.launcher.R

/**
 * Renders the current menu screen's entries as simple icon+label rows,
 * matching wofi/walker's dmenu-style vertical list. Submenu rows get a
 * trailing chevron so it's visually clear they drill down rather than
 * execute immediately.
 */
class OmarchyMenuAdapter(
    private val onSelect: (OmarchyMenuEntry) -> Unit
) : RecyclerView.Adapter<OmarchyMenuAdapter.EntryViewHolder>() {

    private val entries = mutableListOf<OmarchyMenuEntry>()

    fun submitList(newEntries: List<OmarchyMenuEntry>) {
        entries.clear()
        entries.addAll(newEntries)
        notifyDataSetChanged()
    }

    inner class EntryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: android.widget.TextView = view.findViewById(R.id.menuRowIcon)
        val label: android.widget.TextView = view.findViewById(R.id.menuRowLabel)
        val chevron: android.widget.TextView = view.findViewById(R.id.menuRowChevron)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_omarchy_menu_row, parent, false)
        return EntryViewHolder(view)
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        val entry = entries[position]
        holder.icon.text = entry.icon
        holder.label.text = entry.label
        holder.chevron.visibility = if (entry is OmarchyMenuEntry.Submenu) View.VISIBLE else View.GONE
        holder.itemView.setOnClickListener { onSelect(entry) }
    }

    override fun getItemCount(): Int = entries.size
}
