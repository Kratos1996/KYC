@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.chaintech.faceliveness.util

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream

actual class ImageCaptureHelper(private val context: Context) {

    actual fun captureImage(bitmap: Any): String? {
        return try {
            val androidBitmap = bitmap as? Bitmap ?: return null

            val imageFile = File(context.filesDir, "face_image_${System.currentTimeMillis()}.jpg")
            FileOutputStream(imageFile).use { out ->
                androidBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }

            imageFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
