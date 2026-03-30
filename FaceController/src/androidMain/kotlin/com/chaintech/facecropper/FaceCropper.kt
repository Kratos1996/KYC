@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.chaintech.facecropper

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.toRect
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facedetector.FaceDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalStdlibApi::class)
actual class FaceCropper(private val context: Context) {

    private val modelName = "blaze_face_short_range.tflite"

    private val faceDetectorLock = Any()
    private var _faceDetector: FaceDetector? = null

    private val faceDetector: FaceDetector
        get() = synchronized(faceDetectorLock) {
            _faceDetector ?: run {
                val baseOptions = BaseOptions.builder()
                    .setModelAssetPath(modelName)
                    .build()

                val options = FaceDetector.FaceDetectorOptions.builder()
                    .setBaseOptions(baseOptions)
                    .setRunningMode(RunningMode.IMAGE)
                    .setMinDetectionConfidence(0.5f)
                    .build()

                FaceDetector.createFromOptions(context, options).also {
                    _faceDetector = it
                }
            }
        }

    actual suspend fun getCroppedFaceUsingBitmap(
        image: ImageBitmap,
        expandRatio: Float
    ): Result<CroppedFaceResult> = withContext(Dispatchers.IO) {
        try {
            // Create a copy of the bitmap to avoid recycling issues
            val originalBitmap = image.asAndroidBitmap()

            // Check if bitmap is valid
            if (originalBitmap.isRecycled || originalBitmap.width <= 0 || originalBitmap.height <= 0) {
                return@withContext Result.failure(
                    FaceCropperException(ErrorCode.IMAGE_PROCESSING_ERROR, "Invalid bitmap")
                )
            }

            // Create a mutable copy to ensure we have full control
            val bitmap = originalBitmap.copy(originalBitmap.config ?: Bitmap.Config.ARGB_8888, false)

            if (bitmap == null) {
                return@withContext Result.failure(
                    FaceCropperException(ErrorCode.IMAGE_PROCESSING_ERROR, "Failed to copy bitmap")
                )
            }

            // Ensure bitmap is in ARGB_8888 format for MediaPipe
            val mpBitmap = if (bitmap.config != Bitmap.Config.ARGB_8888) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false)?.also {
                    // Recycle the non-ARGB bitmap
                    if (bitmap != originalBitmap) {
                        bitmap.recycle()
                    }
                } ?: bitmap
            } else {
                bitmap
            }

            try {
                // Convert to MPImage - wrap in try-catch for native crashes
                val mpImage = try {
                    BitmapImageBuilder(mpBitmap).build()
                } catch (e: Exception) {
                    throw FaceCropperException(
                        ErrorCode.IMAGE_PROCESSING_ERROR,
                        "Failed to create MPImage: ${e.message}",
                        e
                    )
                }

                // Detect faces - synchronized to prevent concurrent access
                val detectionResult = synchronized(faceDetectorLock) {
                    try {
                        faceDetector.detect(mpImage)
                    } catch (e: Exception) {
                        throw FaceCropperException(
                            ErrorCode.DETECTION_FAILED,
                            "Face detection failed: ${e.message}",
                            e
                        )
                    }
                }

                val faces = detectionResult.detections()

                val result = when {
                    faces.isEmpty() -> {
                        Result.failure(FaceCropperException(ErrorCode.NO_FACE))
                    }
                    faces.size > 1 -> {
                        Result.failure(
                            FaceCropperException(
                                ErrorCode.MULTIPLE_FACES,
                                "Found ${faces.size} faces"
                            )
                        )
                    }
                    else -> {
                        val rect = faces[0].boundingBox().toRect()

                        // Convert Android Rect to our FaceBoundingBox
                        val boundingBox = FaceBoundingBox(
                            left = rect.left,
                            top = rect.top,
                            width = rect.width(),
                            height = rect.height()
                        )

                        // Expand if needed
                        val finalBox = if (expandRatio != 1.0f) {
                            expandBoundingBox(boundingBox, mpBitmap, expandRatio)
                        } else {
                            boundingBox
                        }

                        // Validate and crop
                        if (validateRect(mpBitmap, finalBox)) {
                            val croppedBitmap = try {
                                Bitmap.createBitmap(
                                    mpBitmap,
                                    finalBox.left,
                                    finalBox.top,
                                    finalBox.width,
                                    finalBox.height
                                )
                            } catch (e: Exception) {
                                throw FaceCropperException(
                                    ErrorCode.IMAGE_PROCESSING_ERROR,
                                    "Failed to crop bitmap: ${e.message}",
                                    e
                                )
                            }

                            val confidence = faces[0].categories().firstOrNull()?.score() ?: 0f

                            Result.success(
                                CroppedFaceResult(
                                    bitmap = croppedBitmap.asImageBitmap(),
                                    confidence = confidence,
                                    boundingBox = finalBox
                                )
                            )
                        } else {
                            Result.failure(
                                FaceCropperException(ErrorCode.INVALID_BOUNDS)
                            )
                        }
                    }
                }

                // Clean up the temporary bitmap if it's not the original
                if (mpBitmap != originalBitmap && mpBitmap != bitmap) {
                    mpBitmap.recycle()
                }
                if (bitmap != originalBitmap) {
                    bitmap.recycle()
                }

                result
            } catch (e: FaceCropperException) {
                // Clean up on error
                if (mpBitmap != originalBitmap && mpBitmap != bitmap) {
                    mpBitmap.recycle()
                }
                if (bitmap != originalBitmap) {
                    bitmap.recycle()
                }
                Result.failure(e)
            } catch (e: Exception) {
                // Clean up on error
                if (mpBitmap != originalBitmap && mpBitmap != bitmap) {
                    mpBitmap.recycle()
                }
                if (bitmap != originalBitmap) {
                    bitmap.recycle()
                }
                Result.failure(
                    FaceCropperException(
                        ErrorCode.DETECTION_FAILED,
                        "Unexpected error: ${e.message}",
                        e
                    )
                )
            }
        } catch (e: FaceCropperException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(
                FaceCropperException(
                    ErrorCode.DETECTION_FAILED,
                    "Detection error: ${e.message}",
                    e
                )
            )
        }
    }

    private fun expandBoundingBox(
        box: FaceBoundingBox,
        bitmap: Bitmap,
        ratio: Float
    ): FaceBoundingBox {
        val expandX = (box.width * (ratio - 1) / 2).toInt()
        val expandY = (box.height * (ratio - 1) / 2).toInt()

        return FaceBoundingBox(
            left = maxOf(0, box.left - expandX),
            top = maxOf(0, box.top - expandY),
            width = minOf(bitmap.width - maxOf(0, box.left - expandX), box.width + 2 * expandX),
            height = minOf(bitmap.height - maxOf(0, box.top - expandY), box.height + 2 * expandY)
        )
    }

    private fun validateRect(bitmap: Bitmap, box: FaceBoundingBox): Boolean {
        return box.left >= 0 &&
                box.top >= 0 &&
                box.left + box.width <= bitmap.width &&
                box.top + box.height <= bitmap.height &&
                box.width > 0 &&
                box.height > 0
    }

    private suspend fun saveImageToFile(
        image: ImageBitmap,
        fileName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val bitmap = image.asAndroidBitmap()
            val file = File(context.filesDir, "$fileName.jpg")

            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                stream.flush()
            }

            Result.success(file.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual fun close() {
        synchronized(faceDetectorLock) {
            try {
                _faceDetector?.close()
                _faceDetector = null
            } catch (e: Exception) {
                // Log error
            }
        }
    }
}