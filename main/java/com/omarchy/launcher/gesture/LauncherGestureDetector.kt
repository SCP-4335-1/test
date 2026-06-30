package com.omarchy.launcher.gesture

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import kotlin.math.abs

/**
 * Centralizes all homescreen gesture recognition so HomeActivity doesn't
 * have to juggle raw MotionEvents itself. Wraps Android's GestureDetector
 * for double-tap/long-press and adds a custom upward-fling/swipe check,
 * since GestureDetector's onFling alone is too twitchy for a reliable
 * "open the app drawer" trigger (it fires on small flicks too).
 */
class LauncherGestureDetector(
    context: Context,
    private val callback: Callback
) {

    interface Callback {
        /** Fired on a clear upward swipe of meaningful distance/velocity. */
        fun onSwipeUp()

        /** Fired on a clear downward swipe (e.g. to pull down notifications). */
        fun onSwipeDown()

        /** Double-tap on empty homescreen area, e.g. to lock the screen. */
        fun onDoubleTap()

        /** Long-press on empty homescreen area, to open the context menu. */
        fun onLongPress(x: Float, y: Float)

        /** Plain single tap on empty area -- e.g. to close an open drawer. */
        fun onSingleTapConfirmed()
    }

    private var downX = 0f
    private var downY = 0f
    private var tracking = false

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            callback.onDoubleTap()
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            callback.onLongPress(e.rawX, e.rawY)
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            callback.onSingleTapConfirmed()
            return true
        }

        override fun onDown(e: MotionEvent): Boolean = true
    })

    /**
     * Feed this every touch event the host view receives. Returns true if
     * the event was consumed as part of gesture tracking.
     */
    fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                tracking = true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (tracking) {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (abs(dy) > SWIPE_DISTANCE_THRESHOLD && abs(dy) > abs(dx) * 1.5f) {
                        if (dy < 0) callback.onSwipeUp() else callback.onSwipeDown()
                    }
                }
                tracking = false
            }
        }
        return true
    }

    companion object {
        private const val SWIPE_DISTANCE_THRESHOLD = 80 // dp-ish px threshold, good enough cross-device
    }
}
