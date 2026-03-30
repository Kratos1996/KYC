package com.chaintech.faceliveness.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

object CameraConfig {
     fun calculateBrightness(bitmap: Bitmap): Float {
        var brightness = 0f
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        var sampleCount = 0
        for (i in pixels.indices step 20) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xff
            val g = (pixel shr 8) and 0xff
            val b = pixel and 0xff

            brightness += (0.299f * r + 0.587f * g + 0.114f * b) / 255f
            sampleCount++
        }

        return if (sampleCount > 0) brightness / sampleCount else 0f
    }

     fun adjustBrightness(bitmap: Bitmap, factor: Float): Bitmap {
        val cm = ColorMatrix()
        cm.set(floatArrayOf(
            factor, 0f, 0f, 0f, 0f,
            0f, factor, 0f, 0f, 0f,
            0f, 0f, factor, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))

        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(cm)

        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return result
    }

     fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        return try {
            val yBuffer = imageProxy.planes[0].buffer
            val uBuffer = imageProxy.planes[1].buffer
            val vBuffer = imageProxy.planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 100, out)

            val bitmap = BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())

            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
            }

            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            null
        }
    }
}