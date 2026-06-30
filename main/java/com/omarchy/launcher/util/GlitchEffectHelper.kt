package com.omarchy.launcher.util

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.OvershootInterpolator

/**
 * Tiny, cheap "glitch" micro-interactions: a quick horizontal jitter
 * plus an alpha flicker, used on app-icon tap and drawer open/close to
 * sell the terminal/CRT aesthetic without doing anything expensive like
 * shader-based RGB splitting (which would need a whole separate
 * RenderEffect/AGSL pipeline and a much higher minSdk).
 */
object GlitchEffectHelper {

    fun playTapGlitch(view: View) {
        val jitter = ObjectAnimator.ofFloat(view, View.TRANSLATION_X, 0f, -6f, 5f, -3f, 0f).apply {
            duration = 140
        }
        val flicker = ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0.55f, 1f).apply {
            duration = 140
        }
        AnimatorSet().apply {
            playTogether(jitter, flicker)
            start()
        }
    }

    fun playDrawerOpenGlitch(view: View) {
        view.translationY = view.height * 0.08f
        view.alpha = 0.3f
        val translate = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, view.translationY, 0f).apply {
            duration = 220
            interpolator = OvershootInterpolator(0.6f)
        }
        val fade = ObjectAnimator.ofFloat(view, View.ALPHA, view.alpha, 1f).apply {
            duration = 180
        }
        AnimatorSet().apply {
            playTogether(translate, fade)
            start()
        }
    }
}
