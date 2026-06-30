package com.omarchy.launcher.ui.drawer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.omarchy.launcher.R
import com.omarchy.launcher.data.AppInfo
import kotlin.random.Random

/**
 * Renders each app as a fake `ls -la` row: permission bits + icon + name
 * + a fake byte size. The permission bits and size are deterministically
 * derived from the package name's hash (not actually meaningful) purely
 * for the terminal-output aesthetic -- this is cosmetic flavor, not real
 * file metadata.
 */
class TerminalListAdapter(
    private val onClick: (AppInfo) -> Unit,
    private val onLongClick: (AppInfo, View) -> Unit
) : RecyclerView.Adapter<TerminalListAdapter.RowViewHolder>() {

    private val items = mutableListOf<AppInfo>()

    fun submitList(newItems: List<AppInfo>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class RowViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: android.widget.ImageView = view.findViewById(R.id.itemIcon)
        val label: android.widget.TextView = view.findViewById(R.id.itemLabel)
        val permBits: android.widget.TextView = view.findViewById(R.id.itemPermBits)
        val sizeFake: android.widget.TextView = view.findViewById(R.id.itemSizeFake)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_terminal_row, parent, false)
        return RowViewHolder(view)
    }

    override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
        val app = items[position]
        holder.icon.setImageDrawable(app.icon)
        holder.label.text = app.label
        holder.permBits.text = fakePermBits(app.packageName)
        holder.sizeFake.text = fakeSize(app.packageName)
        holder.itemView.setOnClickListener { onClick(app) }
        holder.itemView.setOnLongClickListener { v ->
            onLongClick(app, v)
            true
        }
    }

    override fun getItemCount(): Int = items.size

    private fun fakePermBits(seed: String): String {
        val rnd = Random(seed.hashCode())
        val perms = listOf("r", "w", "x")
        val sb = StringBuilder("-")
        repeat(9) { i ->
            sb.append(if (rnd.nextInt(10) > 2) perms[i % 3] else "-")
        }
        return sb.toString()
    }

    private fun fakeSize(seed: String): String {
        val rnd = Random(seed.hashCode())
        val kb = 4 + rnd.nextInt(996)
        return "${kb}K"
    }
}
