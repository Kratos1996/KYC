@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.chaintech.facecropper

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import cocoapods.MediaPipeTasksVision.MPPBaseOptions
import cocoapods.MediaPipeTasksVision.MPPCategory
import cocoapods.MediaPipeTasksVision.MPPDetection
import cocoapods.MediaPipeTasksVision.MPPFaceDetector
import cocoapods.MediaPipeTasksVision.MPPFaceDetectorOptions
import cocoapods.MediaPipeTasksVision.MPPFaceDetectorResult
import cocoapods.MediaPipeTasksVision.MPPImage
import cocoapods.MediaPipeTasksVision.MPPRunningMode
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.autoreleasepool
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toCPointer
import kotlinx.cinterop.useContents
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGColorRenderingIntent
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGDataProviderCreateWithData
import platform.CoreGraphics.CGDataProviderRelease
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageCreate
import platform.CoreGraphics.CGImageRelease
import platform.CoreGraphics.kCGBitmapByteOrder32Big
import platform.Foundation.NSBundle
import platform.Foundation.NSError
import platform.UIKit.UIImage
import platform.darwin.NSObject
import kotlin.concurrent.Volatile
import kotlin.math.roundToInt

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class FaceCropper {

    // Thread-safe detector instance
    private var _faceDetector: MPPFaceDetector? = null
    private val detectorLock = SynchronizedObject()

    // Get or create detector with thread safety
    private val faceDetector: MPPFaceDetector?
        get() = synchronized(detectorLock) {
            if (_faceDetector == null) {
                _faceDetector = try {
                    createFaceDetector()
                } catch (e: Exception) {
                    println("Failed to create face detector: ${e.message}")
                    null
                }
            }
            _faceDetector
        }

    /**
     * Create MediaPipe Face Detector with configuration
     */
    private fun createFaceDetector(): MPPFaceDetector {
        // Get model path from bundle
        val modelPath = NSBundle.mainBundle.pathForResource(
            "blaze_face_short_range",
            ofType = "tflite"
        ) ?: throw Exception("Face detection model not found in bundle")

        return autoreleasepool {
            // Configure base options
            val baseOptions = MPPBaseOptions().apply {
                modelAssetPath = modelPath
            }

            // Configure face detector options
            val options = MPPFaceDetectorOptions().apply {
                this.baseOptions = baseOptions
                runningMode = MPPRunningMode.MPPRunningModeImage
                minDetectionConfidence = 0.5f
                minSuppressionThreshold = 0.3f
            }

            // Create detector with error handling
            memScoped {
                val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                val detector = MPPFaceDetector(options = options, error = errorPtr.ptr)

                errorPtr.value?.let { error ->
                    throw Exception(message = "Failed to create FaceDetector: ${error.localizedDescription}")
                }

                detector
            }
        }
    }

    actual suspend fun getCroppedFaceUsingBitmap(
        image: ImageBitmap,
        expandRatio: Float
    ): Result<CroppedFaceResult> = withContext(Dispatchers.Default) {
        // Get detector instance
        val detector = faceDetector ?: return@withContext Result.failure(
            Exception("Face detector not available")
        )

        var mppImage: MPPImage? = null

        try {
            // Convert ImageBitmap to MPPImage with cleanup
            mppImage = autoreleasepool {
                image.toMPImageOptimized()
            } ?: return@withContext Result.failure(
                FaceCropperException(ErrorCode.IMAGE_PROCESSING_ERROR, "Failed to convert to MPPImage")
            )

            // Detect faces using MediaPipe
            val detectionResult =
                detectFacesWithMediaPipe(detector, mppImage) ?: return@withContext Result.failure(
                    FaceCropperException(
                        ErrorCode.DETECTION_FAILED,
                        "Face detection returned null result"
                    )
                )

            @Suppress("UNCHECKED_CAST")
            val faces = detectionResult.detections() as? List<MPPDetection> ?: emptyList()

            when {
                faces.isEmpty() -> {
                    Result.failure(FaceCropperException(ErrorCode.NO_FACE))
                }
                faces.size > 1 -> {
                    Result.failure(
                        FaceCropperException(
                            ErrorCode.MULTIPLE_FACES,
                            "Found ${faces.size} faces"
                        )
                    )
                }
                else -> {
                    processSingleFace(faces.first(), mppImage, image, expandRatio)
                }
            }
        } catch (e: Exception) {
            when (e) {
                is FaceCropperException -> Result.failure(e)
                else -> Result.failure(
                    FaceCropperException(
                        ErrorCode.DETECTION_FAILED,
                        "Face detection error: ${e.message}",
                        e
                    )
                )
            }
        } finally {
            // Clean up MPPImage resources
            mppImage = null
        }
    }

    private fun processSingleFace(
        face: MPPDetection,
        mppImage: MPPImage,
        originalImage: ImageBitmap,
        expandRatio: Float
    ): Result<CroppedFaceResult> {
        return autoreleasepool {
            try {
                // Get bounding box from detection
                val mpBoundingBox = face.boundingBox()

                // MPPImage dimensions
                val imageWidth = mppImage.width().toInt()
                val imageHeight = mppImage.height().toInt()

                // Convert CGRect to our coordinate system
                val boundingBox = mpBoundingBox.useContents {
                    FaceBoundingBox(
                        left = this.origin.x.roundToInt(),
                        top = this.origin.y.roundToInt(),
                        width = this.size.width.roundToInt(),
                        height = this.size.height.roundToInt()
                    )
                }

                // Validate initial bounding box
                if (boundingBox.width <= 0 || boundingBox.height <= 0) {
                    return@autoreleasepool Result.failure(
                        FaceCropperException(ErrorCode.INVALID_BOUNDS, "Invalid face bounding box dimensions")
                    )
                }

                // Expand bounding box if needed
                val finalBox = if (expandRatio != 1.0f) {
                    expandBoundingBox(boundingBox, imageWidth, imageHeight, expandRatio)
                } else {
                    boundingBox
                }

                // Validate bounds
                if (!validateBounds(finalBox, imageWidth, imageHeight)) {
                    return@autoreleasepool Result.failure(
                        FaceCropperException(ErrorCode.INVALID_BOUNDS, "Bounding box out of image bounds")
                    )
                }

                // Crop the face from original image
                val croppedImage = cropImageBitmap(originalImage, finalBox)

                // Get confidence score safely
                @Suppress("UNCHECKED_CAST")
                val categories = face.categories() as? List<MPPCategory>
                val confidence = categories?.firstOrNull()?.score() ?: 0.5f

                Result.success(
                    CroppedFaceResult(
                        bitmap = croppedImage,
                        confidence = confidence,
                        boundingBox = finalBox
                    )
                )
            } catch (e: Exception) {
                Result.failure(
                    FaceCropperException(
                        ErrorCode.DETECTION_FAILED,
                        "Error processing face: ${e.message}",
                        e
                    )
                )
            }
        }
    }

    /**
     * Detect faces using MediaPipe with proper error handling
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun detectFacesWithMediaPipe(
        detector: MPPFaceDetector,
        mppImage: MPPImage
    ): MPPFaceDetectorResult? {
        return autoreleasepool {
            memScoped {
                val errorPtr = alloc<ObjCObjectVar<NSError?>>()

                // Detect faces in the image
                val result = detector.detectImage(
                    image = mppImage,
                    error = errorPtr.ptr
                )

                errorPtr.value?.let { error ->
                    println("Face detection error: ${error.localizedDescription}")
                    // Log additional error info if available
                    println("Error details: ${error.userInfo}")
                    return@autoreleasepool null
                }

                result
            }
        }
    }

    /**
     * Convert ImageBitmap to MPPImage with improved error handling
     */
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun ImageBitmap.toMPImageOptimized(): MPPImage? {
        return autoreleasepool {
            try {
                val skiaBitmap = this.asSkiaBitmap()
                val width = skiaBitmap.width
                val height = skiaBitmap.height

                // Validate dimensions
                if (width <= 0 || height <= 0) {
                    println("Invalid bitmap dimensions: ${width}x${height}")
                    return@autoreleasepool null
                }

                // Get pixel data using peekPixels
                val pixmap = skiaBitmap.peekPixels() ?: run {
                    println("Failed to peek pixels from SkiaBitmap")
                    return@autoreleasepool null
                }

                // Create UIImage directly from pixel data
                memScoped {
                    val colorSpace = CGColorSpaceCreateDeviceRGB()
                    defer { CGColorSpaceRelease(colorSpace) }

                    val bitmapInfo = CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value or
                            kCGBitmapByteOrder32Big

                    // Get the raw pixel pointer
                    val addr = pixmap.addr
                    val rowBytes = pixmap.rowBytes.toInt()

                    // Create data provider from raw pixels
                    val dataProvider = CGDataProviderCreateWithData(
                        info = null,
                        data = addr.toLong().toCPointer<ByteVar>(),
                        size = (rowBytes * height).toULong(),
                        releaseData = null
                    )
                    defer { dataProvider?.let { CGDataProviderRelease(it) } }

                    val cgImage = CGImageCreate(
                        width = width.toULong(),
                        height = height.toULong(),
                        bitsPerComponent = 8u,
                        bitsPerPixel = 32u,
                        bytesPerRow = rowBytes.toULong(),
                        space = colorSpace,
                        bitmapInfo = bitmapInfo,
                        provider = dataProvider,
                        decode = null,
                        shouldInterpolate = false,
                        intent = CGColorRenderingIntent.kCGRenderingIntentDefault
                    )
                    defer { cgImage?.let { CGImageRelease(it) } }

                    cgImage?.let {
                        val uiImage = UIImage.imageWithCGImage(it)

                        val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                        val mppImage = MPPImage(uIImage = uiImage, error = errorPtr.ptr)

                        if (errorPtr.value != null) {
                            println("Error creating MPPImage: ${errorPtr.value?.localizedDescription}")
                            return@autoreleasepool null
                        }

                        return@autoreleasepool mppImage
                    }
                }

                null
            } catch (e: Exception) {
                println("Error in toMPImageOptimized: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }

    private fun cropImageBitmap(originalImage: ImageBitmap, boundingBox: FaceBoundingBox): ImageBitmap {
        val left = boundingBox.left.coerceAtLeast(0)
        val top = boundingBox.top.coerceAtLeast(0)
        val width = boundingBox.width.coerceAtMost(originalImage.width - left)
        val height = boundingBox.height.coerceAtMost(originalImage.height - top)

        // Validate crop dimensions
        if (width <= 0 || height <= 0) {
            throw FaceCropperException(
                ErrorCode.INVALID_BOUNDS,
                "Invalid crop dimensions: ${width}x${height}"
            )
        }

        val cropped = ImageBitmap(width, height)
        val canvas = Canvas(cropped)
        val paint = Paint()

        canvas.drawImageRect(
            image = originalImage,
            srcOffset = IntOffset(left, top),
            srcSize = IntSize(width, height),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(width, height),
            paint = paint
        )

        return cropped
    }

    /**
     * Expand bounding box by ratio with validation
     */
    private fun expandBoundingBox(
        box: FaceBoundingBox,
        imageWidth: Int,
        imageHeight: Int,
        ratio: Float
    ): FaceBoundingBox {
        val expandX = (box.width * (ratio - 1) / 2).toInt()
        val expandY = (box.height * (ratio - 1) / 2).toInt()

        val newLeft = maxOf(0, box.left - expandX)
        val newTop = maxOf(0, box.top - expandY)
        val newRight = minOf(imageWidth, box.left + box.width + expandX)
        val newBottom = minOf(imageHeight, box.top + box.height + expandY)

        return FaceBoundingBox(
            left = newLeft,
            top = newTop,
            width = newRight - newLeft,
            height = newBottom - newTop
        )
    }

    /**
     * Validate if bounding box is within image bounds
     */
    private fun validateBounds(
        box: FaceBoundingBox,
        imageWidth: Int,
        imageHeight: Int
    ): Boolean {
        return box.left >= 0 &&
                box.top >= 0 &&
                box.left + box.width <= imageWidth &&
                box.top + box.height <= imageHeight &&
                box.width > 0 &&
                box.height > 0
    }

    actual fun close() {
        synchronized(detectorLock) {
            _faceDetector = null
        }
    }
}