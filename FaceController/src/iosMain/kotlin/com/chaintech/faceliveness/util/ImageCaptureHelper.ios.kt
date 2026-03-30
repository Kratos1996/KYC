@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.chaintech.faceliveness.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import org.jetbrains.skia.EncodedImageFormat
import platform.CoreImage.CIContext
import platform.CoreImage.CIImage
import platform.CoreImage.createCGImage
import platform.CoreVideo.CVPixelBufferRef
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.writeToFile
import platform.UIKit.UIImage
import org.jetbrains.skia.Image as SkiaImage

@OptIn(ExperimentalForeignApi::class)
actual class ImageCaptureHelper {

    @OptIn(BetaInteropApi::class)
    actual fun captureImage(bitmap: Any): String? {
        return try {

            val imageBitmap = bitmap as? ImageBitmap ?: run {
                println("❌ iOS: Bitmap is not an ImageBitmap")
                return null
            }

            // Convert directly to JPEG data without intermediate UIImage
            val skiaBitmap = imageBitmap.asSkiaBitmap()
            val skiaImage = SkiaImage.makeFromBitmap(skiaBitmap)
            val jpegData = skiaImage.encodeToData(EncodedImageFormat.PNG, 95) ?: run {
                println("❌ iOS: Failed to encode image to JPEG")
                return null
            }

            // Get documents directory
            val documentsPath = NSSearchPathForDirectoriesInDomains(
                NSDocumentDirectory,
                NSUserDomainMask,
                true
            ).firstOrNull() as? String ?: run {
                println("❌ iOS: Cannot access documents directory")
                return null
            }

            // Create file path
            val timestamp = NSDate().timeIntervalSince1970.toLong()
            val fileName = "face_image_$timestamp.jpg"
            val filePath = "$documentsPath/$fileName"

            // Write JPEG data to file
            val bytes = jpegData.bytes
            val nsData = bytes.usePinned {
                NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong())
            }

            val success = nsData.writeToFile(path = filePath, atomically = true)

            if (success) {
                println("✅ iOS: Image saved to: $filePath")
                filePath
            } else {
                println("❌ iOS: Failed to save image to file")
                null
            }
        } catch (e: Exception) {
            println("❌ iOS: Error capturing image: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Helper function to capture image from CVPixelBuffer (for camera frames)
     */
    fun captureImageFromPixelBuffer(pixelBuffer: CVPixelBufferRef): String? {
        return try {
            val ciImage = CIImage.imageWithCVPixelBuffer(pixelBuffer)
            val context = CIContext.contextWithOptions(null)

            val cgImage = context.createCGImage(
                image = ciImage,
                fromRect = ciImage.extent
            )

            if (cgImage == null) {
                println("❌ iOS: Failed to create CGImage")
                return null
            }

            val uiImage = UIImage.imageWithCGImage(cgImage)
            captureImage(uiImage)
        } catch (e: Exception) {
            println("❌ iOS: Error capturing from pixel buffer: ${e.message}")
            null
        }
    }

    /**
     * Helper function to get documents directory path
     */
    fun getDocumentsDirectory(): String? {
        return NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        ).firstOrNull() as? String
    }

    /**
     * Delete image file
     */
    @OptIn(BetaInteropApi::class)
    fun deleteImage(filePath: String): Boolean {
        return try {
            val fileManager = NSFileManager.defaultManager
            memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                val success = fileManager.removeItemAtPath(filePath, error.ptr)

                if (success) {
                    println("✅ iOS: Image deleted: $filePath")
                } else {
                    println("❌ iOS: Failed to delete image: ${error.value?.localizedDescription}")
                }

                success
            }
        } catch (e: Exception) {
            println("❌ iOS: Error deleting image: ${e.message}")
            false
        }
    }
}
