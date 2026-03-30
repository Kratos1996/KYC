package com.chaintech.documentcontroller.documentRectDetector.detectorService

import androidx.compose.ui.graphics.ImageBitmap
import com.chaintech.documentcontroller.documentRectDetector.dataModels.DocumentRegionRect

/**
 * The public interface for the Document Field Detector.
 * Provides capabilities to initialize, detect fields in an image, and release resources.
 */
interface DocumentFieldDetectorService {

    /**
     * Initializes the detector, loading the model and preparing it for inference.
     * This is a long-running operation and must be called from a coroutine.
     */
    suspend fun initialize()

    /**
     * Detects regions of interest (fields) within the provided image data.
     * This is a CPU-intensive operation and must be called from a coroutine.
     */
    suspend fun detectFields(
        image: ImageBitmap,
        imageWidth: Int,
        imageHeight: Int
    ): List<DocumentRegionRect>

    /**
     * Releases all resources used by the detector, including the TFLite model.
     * After calling this, the instance can no longer be used.
     */
    fun dispose()

    // The Factory companion object is the official entry point.
    companion object {
        /**
         * Creates a new instance of the [DocumentFieldDetectorService].
         *
         * @return A new detector instance ready for initialization.
         */
        fun create(): DocumentFieldDetectorService {
            return DocumentFieldDetectorImpl()
        }
    }
}


