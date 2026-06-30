package com.omarchy.launcher.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A terminal-style status line ("guest@omarchy :: 14:32 :: 87%") that
 * sits below the real system status bar (which we deliberately leave
 * visible -- there is no reliable, OEM-safe way for a regular launcher
 * to hide it permanently). This is purely supplementary flavor text,
 * not a replacement for the system bar's own clock/battery icons.
 *
 * Battery percentage comes from a registered ACTION_BATTERY_CHANGED
 * receiver (the only reliable push-based source); the clock just
 * re-renders once a minute via a Handler tick, which is cheap enough
 * to keep running for the lifetime of the home screen.
 */
class StatusLineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : TextView(context, attrs) {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private var batteryPct: Int = -1

    private val mainHandler = Handler(Looper.getMainLooper())
    private val clockTick = object : Runnable {
        override fun run() {
            renderText()
            mainHandler.postDelayed(this, CLOCK_TICK_MS)
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(receivedContext: Context?, intent: Intent?) {
            if (intent == null) return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) {
                batteryPct = (level * 100) / scale
                renderText()
            }
        }
    }

    fun startUpdates() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ throws SecurityException if a receiver is
            // registered without an explicit export flag. ACTION_BATTERY_CHANGED
            // is a system-only sticky broadcast, so NOT_EXPORTED is correct
            // here (no other app should be able to send it to us anyway).
            context.registerReceiver(
                batteryReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(batteryReceiver, filter)
        }
        mainHandler.post(clockTick)
    }

    fun stopUpdates() {
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: IllegalArgumentException) {
            // Already unregistered -- fine, this just guards double-stop calls.
        }
        mainHandler.removeCallbacks(clockTick)
    }

    private fun renderText() {
        val time = timeFormat.format(Date())
        val battery = if (batteryPct >= 0) "$batteryPct%" else "--%"
        text = "guest@omarchy :: $time :: $battery"
    }

    companion object {
        private const val CLOCK_TICK_MS = 60_000L
    }
}
