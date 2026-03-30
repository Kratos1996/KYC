@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.chaintech.kyc.core

import platform.UIKit.UIApplication

actual class KeepScreenOn {

    actual fun enable() {
        UIApplication.sharedApplication.idleTimerDisabled = true
    }

    actual fun disable() {
        UIApplication.sharedApplication.idleTimerDisabled = false
    }
}

actual fun isKeepScreenOnSupported(): Boolean = true