package com.omarchy.launcher.ui.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.recyclerview.widget.LinearLayoutManager
import com.omarchy.launcher.R

object WallpaperPickerDialog {

    fun show(
        context: Context,
        anchor: View,
        onPresetApplied: (Bitmap) -> Unit,
        onPickFromGallery: () -> Unit
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_wallpaper_picker, null, false)
        val popup = PopupWindow(
            view,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        val grid = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.wallpaperPresetGrid)
        grid.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        grid.adapter = WallpaperPresetAdapter { preset ->
            val bitmap = WallpaperPresetRenderer.apply(context, preset)
            popup.dismiss()
            if (bitmap != null) onPresetApplied(bitmap)
        }

        view.findViewById<android.widget.TextView>(R.id.wallpaperSystemPickerButton).setOnClickListener {
            popup.dismiss()
            onPickFromGallery()
        }

        popup.showAtLocation(anchor, android.view.Gravity.CENTER, 0, 0)
    }
}
