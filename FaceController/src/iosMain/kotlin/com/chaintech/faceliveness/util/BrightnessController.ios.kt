@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.chaintech.faceliveness.util


import platform.UIKit.UIScreen

actual class BrightnessController {

    private var originalBrightness: Double = -1.0

    actual fun setBrightness(value: Float) {
        // Save original brightness if not saved yet
        if (originalBrightness == -1.0) {
            originalBrightness = UIScreen.mainScreen.brightness
        }

        UIScreen.mainScreen.brightness = value.toDouble().coerceIn(0.0, 1.0)
    }

    actual fun restoreBrightness() {
        if (originalBrightness != -1.0) {
            UIScreen.mainScreen.brightness = originalBrightness
            originalBrightness = -1.0
        }
    }

    actual fun setMaxBrightness() {
        setBrightness(1.0f)
    }
}
