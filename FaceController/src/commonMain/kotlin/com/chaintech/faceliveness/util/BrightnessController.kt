@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.chaintech.faceliveness.util


expect class BrightnessController {
    fun setBrightness(value: Float) // 0.0f to 1.0f
    fun restoreBrightness()
    fun setMaxBrightness()
}
