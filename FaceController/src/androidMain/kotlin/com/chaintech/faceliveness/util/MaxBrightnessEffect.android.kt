package com.chaintech.faceliveness.util


import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun MaxBrightnessEffect() {
    val context = LocalContext.current
    val activity = context.findActivity()

    DisposableEffect(Unit) {
        val brightnessController = activity?.let { BrightnessController(it) }
        brightnessController?.setMaxBrightness()

        onDispose {
            brightnessController?.restoreBrightness()
        }
    }
}
