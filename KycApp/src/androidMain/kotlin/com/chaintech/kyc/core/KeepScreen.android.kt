@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.chaintech.kyc.core


import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

actual class KeepScreenOn(private val activity: Activity) {

    actual fun enable() {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    actual fun disable() {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

actual fun isKeepScreenOnSupported(): Boolean = true

// Composable helper for Android
@Composable
fun KeepScreenOnComposable() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

private fun android.content.Context.findActivity(): Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}