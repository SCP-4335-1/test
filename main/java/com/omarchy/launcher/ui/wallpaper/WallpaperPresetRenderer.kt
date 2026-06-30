package com.omarchy.launcher.ui.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.DisplayMetrics

/**
 * Draws/decodes wallpaper bitmaps and applies them via WallpaperManager.
 * Functions return the Bitmap directly so the caller can paint it
 * straight into the homescreen's ImageView, sidestepping OEM read-back
 * delays after WallpaperManager.setBitmap().
 */
object WallpaperPresetRenderer {

    /**
     * Gallery photos (e.g. this device's own 50MP camera, ~8160x6120)
     * decoded at full resolution as ARGB_8888 are ~200MB each --
     * guaranteed OutOfMemoryError on a mid-range device. Decoding
     * bounds-only first, then choosing an inSampleSize that fits the
     * actual screen size, keeps memory use proportional to what's ever
     * shown on-screen.
     */
    fun applyFromUri(context: Context, uri: android.net.Uri): Bitmap? {
        return try {
            val metrics = context.resources.displayMetrics
            val targetW = metrics.widthPixels.coerceAtLeast(1)
            val targetH = metrics.heightPixels.coerceAtLeast(1)

            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            } ?: return null

            boundsOptions.inSampleSize = calculateInSampleSize(
                boundsOptions.outWidth, boundsOptions.outHeight, targetW, targetH
            )
            boundsOptions.inJustDecodeBounds = false

            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            } ?: return null

            WallpaperManager.getInstance(context).setBitmap(bitmap)
            bitmap
        } catch (e: OutOfMemoryError) {
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(rawW: Int, rawH: Int, targetW: Int, targetH: Int): Int {
        var inSampleSize = 1
        if (rawW <= 0 || rawH <= 0) return inSampleSize
        var halfW = rawW / 2
        var halfH = rawH / 2
        while ((halfW / inSampleSize) >= targetW && (halfH / inSampleSize) >= targetH) {
            inSampleSize *= 2
        }
        return inSampleSize
    }

    fun apply(context: Context, preset: WallpaperPreset): Bitmap? {
        return try {
            val metrics: DisplayMetrics = context.resources.displayMetrics
            val width = metrics.widthPixels.coerceAtLeast(1)
            val height = metrics.heightPixels.coerceAtLeast(1)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val gradient = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                preset.topColor, preset.bottomColor,
                Shader.TileMode.CLAMP
            )
            val paint = Paint().apply { shader = gradient }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

            val linePaint = Paint().apply {
                color = preset.accentColor
                alpha = 28
                strokeWidth = 2f
            }
            val cols = 6
            for (i in 1 until cols) {
                val x = width * i / cols.toFloat()
                canvas.drawLine(x, 0f, x, height.toFloat(), linePaint)
            }

            WallpaperManager.getInstance(context).setBitmap(bitmap)
            bitmap
        } catch (e: OutOfMemoryError) {
            null
        } catch (e: Exception) {
            null
        }
    }
}
