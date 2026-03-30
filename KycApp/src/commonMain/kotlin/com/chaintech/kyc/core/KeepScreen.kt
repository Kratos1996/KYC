@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.chaintech.kyc.core


expect class KeepScreenOn {
    fun enable()
    fun disable()
}

expect fun isKeepScreenOnSupported(): Boolean