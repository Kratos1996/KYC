@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.chaintech.faceliveness.util

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.Flow

// Expect/Actual pattern for platform-specific implementations
expect class FaceLandmarkerHelper {
    fun initialize(
        onError: (String) -> Unit
    )

    fun detectAsync(
        bitmap: ImageBitmap,
        frameTimeMs: Long
    )

    val resultsFlow: Flow<FacePosition?>

    fun close()
}
