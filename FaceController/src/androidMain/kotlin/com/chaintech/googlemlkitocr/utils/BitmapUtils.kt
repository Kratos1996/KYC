package com.chaintech.googlemlkitocr.utils

import android.graphics.*
import java.io.ByteArrayOutputStream
import androidx.core.graphics.createBitmap

object BitmapUtils {
    fun preprocessForOcr(bitmap: Bitmap): Bitmap {
        // Convert to grayscale and apply contrast enhancement
        val width = bitmap.width
        val height = bitmap.height

        val result = createBitmap(width, height)
        val canvas = Canvas(result)

        // Apply grayscale
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                setSaturation(0f)
            })
        }

        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        // Enhance contrast
        return enhanceContrast(result)
    }

    private fun enhanceContrast(bitmap: Bitmap): Bitmap {
        val colorMatrix = ColorMatrix(floatArrayOf(
            1.5f, 0f, 0f, 0f, -50f,
            0f, 1.5f, 0f, 0f, -50f,
            0f, 0f, 1.5f, 0f, -50f,
            0f, 0f, 0f, 1f, 0f
        ))

        val result = createBitmap(bitmap.width, bitmap.height)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }

        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }
}