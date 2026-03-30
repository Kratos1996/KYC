@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.chaintech.faceliveness.util

import android.app.Activity
import android.view.WindowManager

actual class BrightnessController(private val activity: Activity) {

    private var originalBrightness: Float = -1f

    actual fun setBrightness(value: Float) {
        // Save original brightness if not saved yet
        if (originalBrightness == -1f) {
            originalBrightness = activity.window.attributes.screenBrightness
        }

        val layoutParams = activity.window.attributes
        layoutParams.screenBrightness = value.coerceIn(0f, 1f)
        activity.window.attributes = layoutParams
    }

    actual fun restoreBrightness() {
        if (originalBrightness != -1f) {
            val layoutParams = activity.window.attributes
            layoutParams.screenBrightness = originalBrightness
            activity.window.attributes = layoutParams
            originalBrightness = -1f
        }
    }

    actual fun setMaxBrightness() {
        setBrightness(1.0f)
    }
}

// Extension to get Activity from Context
fun android.content.Context.findActivity(): Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
