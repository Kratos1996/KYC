@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.chaintech.faceliveness.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import cocoapods.MediaPipeTasksVision.*
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.autoreleasepool
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toCPointer
import kotlinx.cinterop.value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import platform.CoreGraphics.CGColorRenderingIntent
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGDataProviderCreateWithData
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageCreate
import platform.CoreGraphics.kCGBitmapByteOrder32Big
import platform.Foundation.NSBundle
import platform.Foundation.NSError
import platform.UIKit.UIImage
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
actual class FaceLandmarkerHelper {
    private var faceLandmarker: MPPFaceLandmarker? = null
    private val resultChannel = Channel<MPPFaceLandmarkerResult?>(Channel.UNLIMITED)
    private val poseCalculator = FacePoseCalculator()
    private var streamDelegate: MPPFaceLandmarkerLiveStreamDelegateProtocol? = null

    actual val resultsFlow: Flow<FacePosition?> = resultChannel.receiveAsFlow().map { result ->
        result?.let {
            poseCalculator.calculatePose(it)
        }
    }

    @OptIn(BetaInteropApi::class)
    actual fun initialize(onError: (String) -> Unit) {
        try {
            val modelPath = NSBundle.mainBundle.pathForResource("face_landmarker", "task")
                ?: throw Exception("Model file not found")

            val baseOptions = MPPBaseOptions()
            baseOptions.modelAssetPath = modelPath

            val options = MPPFaceLandmarkerOptions()
            options.baseOptions = baseOptions
            options.runningMode = MPPRunningMode.MPPRunningModeLiveStream // MPPRunningModeImage, MPPRunningModeVideo, MPPRunningModeLiveStream
            options.minFaceDetectionConfidence = 0.3f
            options.outputFacialTransformationMatrixes = false
            options.outputFaceBlendshapes = false
            options.minFacePresenceConfidence = 0.3f
            options.minTrackingConfidence = 0.3f
            options.numFaces = 1

            // Store the delegate as a property to keep it from being garbage collected
            streamDelegate = object : NSObject(), MPPFaceLandmarkerLiveStreamDelegateProtocol {
                override fun faceLandmarker(
                    faceLandmarker: MPPFaceLandmarker,
                    didFinishDetectionWithResult: MPPFaceLandmarkerResult?,
                    timestampInMilliseconds: Long,
                    error: NSError?
                ) {
                    error?.let {
                        println("MediaPipVisionTask iOS: didFinishDetectionWithResult: error -> ${it.localizedDescription}")
                        onError(it.localizedDescription)
                        return
                    }
                    resultChannel.trySend(didFinishDetectionWithResult)
                }
            }

            options.faceLandmarkerLiveStreamDelegate = streamDelegate

            // Also handle errors in initialization
            memScoped {
                val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                faceLandmarker = MPPFaceLandmarker(options = options, error = errorPtr.ptr)

                errorPtr.value?.let { error ->
                    throw Exception("Failed to create FaceLandmarker: ${error.localizedDescription}")
                }
            }

            println("MediaPipVisionTask iOS: Initialization successful")
        } catch (e: Exception) {
            onError("Initialization failed: ${e.message}")
        }
    }

    @OptIn(BetaInteropApi::class)
    actual fun detectAsync(bitmap: ImageBitmap, frameTimeMs: Long) {
        val mpImage = bitmap.toMPImageOptimized() ?: run {
            println("MediaPipVisionTask iOS: Failed to convert ImageBitmap to MPPImage")
            return
        }

        memScoped {
            CoroutineScope(Dispatchers.Default).launch {
                autoreleasepool {
                    val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                    faceLandmarker?.detectAsyncImage(mpImage, frameTimeMs, errorPtr.ptr)
                    // Check if the operation failed
                    if (errorPtr.value != null) {
                        println("MediaPipVisionTask iOS: Error detectAsync: ${errorPtr.value?.localizedDescription}")
                    }
                }
            }
        }
    }

    actual fun close() {
        faceLandmarker = null
        resultChannel.close()
        poseCalculator.release()
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun ImageBitmap.toMPImageOptimized(): MPPImage? {
        return autoreleasepool {
            try {
                val skiaBitmap = this.asSkiaBitmap()
                val width = skiaBitmap.width
                val height = skiaBitmap.height

                // Get pixel data using peekPixels
                val pixmap = skiaBitmap.peekPixels() ?: run {
                    println("Failed to peek pixels from SkiaBitmap")
                    return@autoreleasepool null
                }

                // Create UIImage directly from pixel data
                memScoped {
                    val colorSpace = CGColorSpaceCreateDeviceRGB()
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
                null
            }
        }
    }
}