package com.chaintech.googlemlkitocr

import androidx.compose.ui.graphics.ImageBitmap

interface TextRecognizer {
    /**
     * Process image and extract text
     */
    suspend fun processImage(imageData: ImageBitmap): String

    /**
     * Release resources
     */
    fun release()
}

/**
 * Factory function to create platform-specific implementation
 */
expect fun createTextRecognizer(): TextRecognizer

sealed class OcrException(message: String) : Exception(message) {
    class ImageProcessingException(message: String) : OcrException(message)
    class RecognitionException(message: String) : OcrException(message)
    class ConfigurationException(message: String) : OcrException(message)
    class PermissionException(message: String) : OcrException(message)
}