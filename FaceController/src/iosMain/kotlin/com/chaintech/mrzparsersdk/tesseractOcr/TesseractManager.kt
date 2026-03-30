package com.chaintech.mrzparsersdk.tesseractOcr

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import cocoapods.TesseractOCR.G8OCREngineMode
import cocoapods.TesseractOCR.G8PageSegmentationMode
import cocoapods.TesseractOCR.G8Tesseract
import cocoapods.TesseractOCR.g8_blackAndWhite
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.autoreleasepool
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.EncodedImageFormat
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextCreateImage
import platform.CoreGraphics.CGColorSpaceCreateDeviceGray
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage
import org.jetbrains.skia.Image as SkiaImage

actual fun createTesseractManager(): TesseractManager {
    return TesseractManager()
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class TesseractManager {
    private var tesseract: G8Tesseract? = null

    actual suspend fun initialize(language: String): Boolean {
        return try {
            println("TesseractManager: iOS - Initializing Tesseract...")
            tesseract = G8Tesseract(
                language = language,
                engineMode = G8OCREngineMode.G8OCREngineModeLSTMOnly
            ).apply {
                // AutoOSD automatically detects orientation and script, rotating the image internally.
                pageSegmentationMode = G8PageSegmentationMode.G8PageSegmentationModeAuto
                maximumRecognitionTime = 60.0
                // Configure for better accuracy
                setVariableValue("tessedit_char_blacklist", forKey = "classify_bln_numeric_mode")
            }

            if (tesseract == null) {
                println("ERROR: Failed to initialize Tesseract with custom path")
                return false
            }

            println("TesseractManager: iOS - initialized successfully.")
            tesseract != null
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    actual suspend fun extractTextFromBitmap(imageBitmap: ImageBitmap): String? =
        withContext(Dispatchers.Default) {
            if (tesseract == null) {
                println("TesseractManager: ERROR - Tesseract is null!")
                return@withContext null
            }

            val image = imageBitmap.toUIImage() ?: run {
                println("TesseractManager: ERROR - Failed to convert ImageBitmap to UIImage")
                return@withContext null
            }

            autoreleasepool {
                try {
                    // Set the image
                    tesseract?.image = image.g8_blackAndWhite() ?: image

                    // Perform recognition
                    val success = tesseract?.recognize() ?: false

                    if (!success) {
                        println("TesseractManager: Recognition returned false")
                        return@autoreleasepool null
                    }

                    // Get recognized text
                    val tesseractText = tesseract?.recognizedText
                    println("TesseractManager: iOS - Recognized: $tesseractText")
                    tesseractText
                } catch (e: Exception) {
                    println("TesseractManager: Recognition error - ${e.message}")
                    e.printStackTrace()
                    null
                }
            }
        }

    actual suspend fun setPageSegmentationMode(mode: PageSegmentationMode) {
//        val iosMode = when (mode) {
//            PageSegmentationMode.AUTO -> G8PageSegmentationMode.G8PageSegmentationModeAuto
//            PageSegmentationMode.SINGLE_BLOCK -> G8PageSegmentationMode.G8PageSegmentationModeSingleBlock
//            PageSegmentationMode.SINGLE_LINE -> G8PageSegmentationMode.G8PageSegmentationModeSingleLine
//            PageSegmentationMode.SINGLE_WORD -> G8PageSegmentationMode.G8PageSegmentationModeSingleWord
//            PageSegmentationMode.SINGLE_CHAR -> G8PageSegmentationMode.G8PageSegmentationModeSingleChar
//        }
//        tesseract?.pageSegmentationMode = iosMode
    }

    actual suspend fun release() {
        tesseract = null
    }

    fun ImageBitmap.toUIImage(): UIImage? {
        return autoreleasepool {
            try {
                // Step 1: Convert to native UIImage
                val skiaBitmap = this.asSkiaBitmap()
                val skiaImage = SkiaImage.makeFromBitmap(skiaBitmap)

                // Encode to JPEG (fast)
                val data =
                    skiaImage.encodeToData(format = EncodedImageFormat.PNG, quality = 100) ?: run {
                        println("TesseractManager: iOS - ImageBitmap.toUIImage() - Failed to encode image to JPEG")
                        return@autoreleasepool null
                    }

                val bytes = data.bytes
                val nsData = bytes.usePinned {
                    NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong())
                }

                val uiImage = UIImage.imageWithData(nsData) ?: return@autoreleasepool null
                uiImage
            } catch (e: Exception) {
                println("TesseractManager: iOS - ImageBitmap.toUIImage() - Exception - ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }

    private fun preprocessImage(image: UIImage): UIImage {
        return autoreleasepool {
            // Convert to grayscale
            val grayscale = convertToGrayscale(image)
            return@autoreleasepool grayscale
        }
    }

    private fun convertToGrayscale(image: UIImage): UIImage {
        val imageRect = CGRectMake(
            0.0,
            0.0,
            image.size.useContents { width },
            image.size.useContents { height })
        val colorSpace = CGColorSpaceCreateDeviceGray()

        val context = CGBitmapContextCreate(
            null,
            image.size.useContents { width.toULong() },
            image.size.useContents { height.toULong() },
            8u,
            0u,
            colorSpace,
            CGImageAlphaInfo.kCGImageAlphaNone.value
        )

        context?.let { ctx ->
            image.CGImage?.let { cgImage ->
                CGContextDrawImage(ctx, imageRect, cgImage)
                CGBitmapContextCreateImage(ctx)?.let { grayscaleCGImage ->
                    return UIImage.imageWithCGImage(grayscaleCGImage)
                }
            }
        }

        return image
    }
}