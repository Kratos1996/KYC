package com.chaintech.googlemlkitocr

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import cocoapods.MLKitTextRecognition.MLKText
import cocoapods.MLKitTextRecognition.MLKTextRecognizer
import cocoapods.MLKitTextRecognition.textRecognizer
import cocoapods.MLKitVision.MLKVisionImage
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.autoreleasepool
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import objcnames.protocols.MLKCompatibleImageProtocol
import org.jetbrains.skia.EncodedImageFormat
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import org.jetbrains.skia.Image as SkiaImage

actual fun createTextRecognizer(): TextRecognizer {
    return IOSTextRecognizer()
}

class IOSTextRecognizer() : TextRecognizer {

    @OptIn(ExperimentalForeignApi::class)
    private var textRecognizer: MLKTextRecognizer? = null
    private val lock = SynchronizedObject()

    @OptIn(ExperimentalForeignApi::class)
    private fun getRecognizer(): MLKTextRecognizer {
        synchronized(lock) {
            if (textRecognizer == null) {
                textRecognizer = MLKTextRecognizer.textRecognizer()
            }
            return textRecognizer!!
        }
    }

    @OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
    override suspend fun processImage(imageData: ImageBitmap): String = withContext(Dispatchers.IO) {
        try {
            val uiImage = imageData.toUIImage()
            val visionImage = MLKVisionImage(image = uiImage)
            visionImage.orientation = uiImage.imageOrientation

            val result = processText(visionImage)

            result.text()
        } catch (e: Exception) {
            // If recognizer failed, reset and retry once
            if (e.message?.contains("failed") == true || e.message?.contains("error") == true) {
                retry(imageData)
            }
            throw OcrException.RecognitionException("Failed to process image: ${e.message}")
        }
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private suspend fun retry(imageData: ImageBitmap): String = withContext(Dispatchers.IO) {
        synchronized(lock) {
            textRecognizer = null
        }
        // Retry
        val uiImage = imageData.toUIImage()
        val visionImage = MLKVisionImage(image = uiImage)
        visionImage.orientation = uiImage.imageOrientation

        val result = processText(visionImage)
        return@withContext result.text()
    }

    @OptIn(ExperimentalForeignApi::class)
    private suspend fun processText(visionImage: MLKVisionImage): MLKText =
        suspendCancellableCoroutine { continuation ->
            val compatibleImage = visionImage as MLKCompatibleImageProtocol
            getRecognizer().processImage(compatibleImage) { result, error ->
                when {
                    continuation.isActive && error != null -> {
                        continuation.resumeWithException(
                            Exception(error.localizedDescription)
                        )
                    }
                    continuation.isActive && result != null -> {
                        continuation.resume(result)
                    }
                }
            }
        }

    @OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
    private fun ImageBitmap.toUIImage(): UIImage = autoreleasepool {
        val skiaBitmap = this@toUIImage.asSkiaBitmap()
        val skiaImage = SkiaImage.makeFromBitmap(skiaBitmap)

        val data = skiaImage.encodeToData(EncodedImageFormat.PNG, 100)
            ?: throw IllegalStateException("Failed to encode image")

        val nsData = data.bytes.usePinned { pinned ->
            NSData.create(
                bytes = pinned.addressOf(0),
                length = data.bytes.size.toULong()
            )
        }

        UIImage.imageWithData(nsData)
            ?: throw IllegalStateException("Failed to create UIImage")
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun release() {
        synchronized(lock) {
            textRecognizer = null
        }
    }
}
