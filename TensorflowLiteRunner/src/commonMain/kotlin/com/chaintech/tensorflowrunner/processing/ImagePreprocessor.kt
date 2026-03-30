package com.chaintech.tensorflowrunner.processing

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap

data class ImageSize(
    val width: Int,
    val height: Int
)

/**
 * Image preprocessing in common code
 */
class ImagePreprocessor {

    /**
     * Preprocess image for model input
     * Handles resizing and normalization
     */
    fun preprocessImage(
        imageData: ByteArray,
        imageSize: ImageSize,
        targetSize: ImageSize
    ): FloatArray {
        // Resize image to target size
        val resizedImage = resizeImage(
            imageData = imageData,
            originalSize = imageSize,
            targetSize = targetSize
        )

        // Normalize pixel values to [0, 1]
        return normalizeImageToFloat(resizedImage)
    }

    private fun resizeImage(
        imageData: ByteArray,
        originalSize: ImageSize,
        targetSize: ImageSize
    ): ByteArray {
        val srcWidth = originalSize.width
        val srcHeight = originalSize.height

        val dstWidth = targetSize.width
        val dstHeight = targetSize.height

        val output = ByteArray(dstWidth * dstHeight * 3)

        for (y in 0 until dstHeight) {
            for (x in 0 until dstWidth) {
                val srcX = (x * srcWidth) / dstWidth
                val srcY = (y * srcHeight) / dstHeight
                val srcIdx = (srcY * srcWidth + srcX) * 3
                val dstIdx = (y * dstWidth + x) * 3

                if (srcIdx + 2 < imageData.size && dstIdx + 2 < output.size) {
                    output[dstIdx] = imageData[srcIdx]
                    output[dstIdx + 1] = imageData[srcIdx + 1]
                    output[dstIdx + 2] = imageData[srcIdx + 2]
                }
            }
        }
        return output
    }

    /**
     * Normalizes byte values (0-255) to float values (0.0-1.0)
     * @param imageData RGB byte array
     * @return Float array with values in range [0.0, 1.0]
     */
    private fun normalizeImageToFloat(imageData: ByteArray): FloatArray {
        val normalized = FloatArray(imageData.size)
        for (i in imageData.indices) {
            normalized[i] = (imageData[i].toInt() and 0xFF) / 255.0f
        }
        return normalized
    }

    fun resizeImage(
        image: ImageBitmap,
        targetSize: ImageSize
    ): ByteArray {
        val srcWidth = image.width
        val srcHeight = image.height
        val imageData = image.toRGBBytes()

        val dstWidth = targetSize.width
        val dstHeight = targetSize.height

        val output = ByteArray(dstWidth * dstHeight * 3)

        for (y in 0 until dstHeight) {
            for (x in 0 until dstWidth) {
                val srcX = (x * srcWidth) / dstWidth
                val srcY = (y * srcHeight) / dstHeight
                val srcIdx = (srcY * srcWidth + srcX) * 3
                val dstIdx = (y * dstWidth + x) * 3

                if (srcIdx + 2 < imageData.size && dstIdx + 2 < output.size) {
                    output[dstIdx] = imageData[srcIdx]
                    output[dstIdx + 1] = imageData[srcIdx + 1]
                    output[dstIdx + 2] = imageData[srcIdx + 2]
                }
            }
        }
        return output
    }
}

fun ImageBitmap.toRGBBytes(): ByteArray {
    val pixelMap = this.toPixelMap()
    val width = this.width
    val height = this.height
    val bytes = ByteArray(width * height * 3)

    var index = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            val color = pixelMap[x, y] // Convert Int to Color
            bytes[index++] = (color.red * 255).toInt().toByte()   // Red
            bytes[index++] = (color.green * 255).toInt().toByte() // Green
            bytes[index++] = (color.blue * 255).toInt().toByte()  // Blue
        }
    }

    return bytes
}