package com.chaintech.kyc.theme

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
internal actual fun StatusBarColor(statusBarColor: Color) {
    val view = LocalView.current
    val systemBarColor = statusBarColor.toArgb()
    val navigationBarColor = Color.Transparent.toArgb()
    SideEffect {
        val window = (view.context as Activity).window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = systemBarColor
        window.navigationBarColor = navigationBarColor
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }
}
