@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.chaintech.faceliveness.util

expect class VideoRecorder {
    fun startRecording(
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    )

    fun stopRecording()
    fun getVideoUri(): String?
    fun release()
}
