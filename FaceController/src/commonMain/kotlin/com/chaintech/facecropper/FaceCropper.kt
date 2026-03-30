@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.chaintech.facecropper

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Face detection and cropping utility
 * Platform-specific implementations use native APIs for best performance
 */
expect class FaceCropper {
    /**
     * Detect and crop face from image
     * @param image Input image bitmap
     * @param expandRatio Ratio to expand the face bounding box (1.0 = no expansion)
     * @return Result containing cropped face or error
     */
    suspend fun getCroppedFaceUsingBitmap(
        image: ImageBitmap,
        expandRatio: Float = 1.2f
    ): Result<CroppedFaceResult>


    /**
     * Release resources
     */
    fun close()
}

/**
 * Result data class for cropped face
 */
data class CroppedFaceResult(
    val bitmap: ImageBitmap,
    val confidence: Float,
    val boundingBox: FaceBoundingBox
)

/**
 * Platform-agnostic bounding box
 */
data class FaceBoundingBox(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int
)

/**
 * Error codes for face cropping operations
 */
enum class ErrorCode(val message: String) {
    MULTIPLE_FACES("Multiple faces detected"),
    NO_FACE("No face detected"),
    INVALID_BOUNDS("Face bounds exceed image dimensions"),
    DETECTION_FAILED("Face detection failed"),
    DETECTOR_CLOSED("Detector has been closed"),
    IMAGE_PROCESSING_ERROR("Image processing error"),
    BITMAP_RECYCLED("Input bitmap has been recycled")
}

/**
 * Custom exception for face cropper errors
 */
class FaceCropperException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.message,
    override val cause: Throwable? = null
) : Exception(message, cause)