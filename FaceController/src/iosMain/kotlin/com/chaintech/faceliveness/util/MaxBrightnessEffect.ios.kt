package com.chaintech.faceliveness.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

@Composable
actual fun MaxBrightnessEffect() {
    DisposableEffect(Unit) {
        val brightnessController = BrightnessController()
        brightnessController.setMaxBrightness()

        onDispose {
            brightnessController.restoreBrightness()
        }
    }
}
