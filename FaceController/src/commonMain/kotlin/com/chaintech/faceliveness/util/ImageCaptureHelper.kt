@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.chaintech.faceliveness.util

expect class ImageCaptureHelper {
    fun captureImage(bitmap: Any): String?
}