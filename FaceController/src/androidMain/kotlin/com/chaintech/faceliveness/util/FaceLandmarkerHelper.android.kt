@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.chaintech.faceliveness.util

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow

actual class FaceLandmarkerHelper(private val context: Context) {
    private var faceLandmarker: FaceLandmarker? = null
    private val resultChannel = Channel<FaceLandmarkerResult?>(Channel.UNLIMITED)
    private val poseCalculator = FacePoseCalculator()

    actual val resultsFlow: Flow<FacePosition?> = resultChannel.receiveAsFlow()
        .map { result ->
            result?.let { poseCalculator.calculatePose(it) }
        }

    actual fun initialize(onError: (String) -> Unit) {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("face_landmarker.task")
                .build()

            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setMinFaceDetectionConfidence(0.3f)
                .setMinFacePresenceConfidence(0.3f)
                .setMinTrackingConfidence(0.3f)
                .setNumFaces(1)
                .setOutputFaceBlendshapes(false)
                .setOutputFacialTransformationMatrixes(false)
                .setResultListener { result, _ ->
                    resultChannel.trySend(result)
                }
                .setErrorListener { error ->
                    Log.e("FaceLandmarker", "Error: ${error.message}")
                    onError(error.message ?: "Unknown error")
                }
                .build()

            faceLandmarker = FaceLandmarker.createFromOptions(context, options)
            Log.d("FaceLandmarker", "Initialized successfully")
        } catch (e: Exception) {
            Log.e("FaceLandmarker", "Initialization failed", e)
            onError("Failed to initialize: ${e.message}")
        }
    }

    actual fun detectAsync(bitmap: ImageBitmap, frameTimeMs: Long) {
        try {
            val androidBitmap = bitmap.asAndroidBitmap()
            val mpImage = BitmapImageBuilder(androidBitmap).build()
            faceLandmarker?.detectAsync(mpImage, frameTimeMs)
        } catch (e: Exception) {
            Log.e("FaceLandmarker", "Detection error", e)
        }
    }

    actual fun close() {
        faceLandmarker?.close()
        faceLandmarker = null
        resultChannel.close()
        poseCalculator.release()
    }
}
