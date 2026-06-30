package com.omarchy.launcher.ui.wallpaper

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.omarchy.launcher.R

class WallpaperPresetAdapter(
    private val onSelect: (WallpaperPreset) -> Unit
) : RecyclerView.Adapter<WallpaperPresetAdapter.VH>() {

    private val items = WallpaperPresets.all

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val swatch: View = view.findViewById(R.id.presetSwatch)
        val label: android.widget.TextView = view.findViewById(R.id.presetLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_wallpaper_preset, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val preset = items[position]
        holder.label.text = preset.label
        val gradient = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(preset.topColor, preset.bottomColor)
        ).apply { cornerRadius = 10f }
        holder.swatch.background = gradient
        holder.itemView.setOnClickListener { onSelect(preset) }
    }

    override fun getItemCount(): Int = items.size
}
