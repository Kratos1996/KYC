@file:OptIn(BetaInteropApi::class)

package com.chaintech.faceliveness.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chaintech.faceliveness.base.FaceConfig
import com.chaintech.faceliveness.util.ImageCaptureHelper
import com.chaintech.faceliveness.util.OrientationListener
import com.chaintech.faceliveness.util.VideoRecorder
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.autoreleasepool
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceDiscoverySession
import platform.AVFoundation.AVCaptureDevicePositionFront
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInWideAngleCamera
import platform.AVFoundation.AVCaptureFocusModeContinuousAutoFocus
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.focusMode
import platform.AVFoundation.isFocusModeSupported
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIColor
import platform.UIKit.UIView
import org.jetbrains.skia.Bitmap as SkiaBitmap
import org.jetbrains.skia.Canvas as SkiaCanvas
import org.jetbrains.skia.ColorFilter as SkiaColorFilter
import org.jetbrains.skia.ColorMatrix as SkiaColorMatrix
import org.jetbrains.skia.Image as SkiaImage
import org.jetbrains.skia.Paint as SkiaPaint

@OptIn(ExperimentalForeignApi::class, DelicateCoroutinesApi::class)
@Composable
actual fun CameraView(
    faceConfig: FaceConfig,
    onProcessComplete: (image: ImageBitmap, imagePath: String, videoPath: String) -> Unit,
    onClose:()->Unit,
    modifier: Modifier
) {
    val viewModel: FaceDetectionViewModel = koinInject()
    val videoRecorder: VideoRecorder = koinInject()
    val imageCaptureHelper: ImageCaptureHelper = koinInject()
    val message by viewModel.message.collectAsState()
    val borderState by viewModel.borderState.collectAsState()
    val processComplete by viewModel.processDone.collectAsState()

    // State to hold the current AVCaptureDevice
    var currentCameraDevice by remember { mutableStateOf(findCameraDevice()) }

    val camera = currentCameraDevice ?: return // Don't render if no camera is found yet

    val coordinator = remember(camera) {
        CameraCaptureCoordinator(
            camera = camera,
            onFrameCaptured = { image ->
                image?.let {
                    autoreleasepool {
                        processImageBitmapWithBrightness(it, viewModel)
                    }
                }
            }
        )
    }

    // Configure autofocus
    if (camera.isFocusModeSupported(AVCaptureFocusModeContinuousAutoFocus)) {
        camera.lockForConfiguration(null)
        camera.focusMode = AVCaptureFocusModeContinuousAutoFocus
        camera.unlockForConfiguration()
    }
    // Reset ViewModel state when screen is first displayed
    LaunchedEffect(Unit) {
        viewModel.faceConfigValue.value = faceConfig
        viewModel.resetForNewSession()
    }

    LaunchedEffect(processComplete) {
        if (processComplete) {
            val videoPath = viewModel.finalSelfieVideo.value ?: ""
            val imagePath = "" // Will be set from viewModel
            val imageBitmap = viewModel.getCurrentBitmap() as? ImageBitmap
            if (imageBitmap != null) {
                onProcessComplete(imageBitmap, imagePath, videoPath)
                viewModel.processDone(true)
            } else {
                viewModel.processDone(false)
            }
        }
    }

    DisposableEffect(Unit) {
        viewModel.initializeFaceDetection()
        val listener = OrientationListener { orientation ->
            coordinator.setCurrentOrientation(orientation)
        }

        coordinator.startCapturingFrames()

        listener.register()

        onDispose {
            listener.unregister()
            GlobalScope.launch(Dispatchers.IO) {
                coordinator.stopCapturingFrames()
                coordinator.stopSession()
            }
            videoRecorder.release()
            viewModel.cleanup()
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        // Camera Box
        Box(
            modifier = Modifier.size(250.dp).padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Camera Preview with proper sizing
            CameraPreviewView(
                modifier = Modifier.size(270.dp),
                coordinator = coordinator,
                videoRecorder = videoRecorder,
                cameraPreviewSize = 270.0
            )

            CircularCameraOverlay(
                borderState = borderState,
                modifier = Modifier.size(230.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Brightness Indicator
        Text(
            text = "Brightness: ${viewModel.getBrightnessPercentage()}% - ${viewModel.getBrightnessStatus()}",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Message
        Text(
            text = message,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            color = Color.White,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
private fun CameraPreviewView(
    modifier: Modifier = Modifier,
    coordinator: CameraCaptureCoordinator,
    videoRecorder: VideoRecorder,
    cameraPreviewSize: Double
) {
    val rect = CGRectMake(0.0, 0.0,cameraPreviewSize,cameraPreviewSize)

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.ui.viewinterop.UIKitView(
            factory = {
                val previewContainer = UIView()
                previewContainer.setFrame(rect)
                previewContainer.backgroundColor = UIColor.blackColor
                previewContainer.clipsToBounds = true

                coordinator.prepare(previewContainer.layer)
                videoRecorder.setupVideoOutput(coordinator.captureSession)
                coordinator.setFrame(rect)

                previewContainer
            },
            modifier = modifier.fillMaxSize(),
            update = { }
        )
    }
}

private fun findCameraDevice(): AVCaptureDevice? {
    val deviceType = AVCaptureDeviceTypeBuiltInWideAngleCamera // Most common camera type
    val mediaType = AVMediaTypeVideo
    val avPosition = AVCaptureDevicePositionFront

    val discoverySession = AVCaptureDeviceDiscoverySession.discoverySessionWithDeviceTypes(
        deviceTypes = listOf(deviceType),
        mediaType = mediaType,
        position = avPosition
    )

    return discoverySession.devices.firstOrNull() as? AVCaptureDevice
}

private fun processImageBitmapWithBrightness(
    bitmap: ImageBitmap,
    viewModel: FaceDetectionViewModel
) {
    try {
        val brightness = calculateBrightness(bitmap)
        viewModel.updateBrightness(brightness)

        val adjustedBitmap = when {
            brightness < 0.35f -> adjustBrightness(bitmap, 1.4f)
            brightness > 0.85f -> adjustBrightness(bitmap, 0.8f)
            else -> bitmap
        }

        viewModel.setCurrentBitmap(adjustedBitmap)
        viewModel.processFrame(adjustedBitmap)

    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun calculateBrightness(imageBitmap: ImageBitmap): Float {
    val skiaBitmap = imageBitmap.asSkiaBitmap()
    val width = skiaBitmap.width
    val height = skiaBitmap.height

    var brightness = 0f
    var sampleCount = 0

    // Get pixel data
    val pixels = IntArray(width * height)
    val pixmap = skiaBitmap.peekPixels()

    if (pixmap != null) {
        // Read all pixels into array
        for (y in 0 until height) {
            for (x in 0 until width) {
                pixels[y * width + x] = pixmap.getColor(x, y)
            }
        }

        // Sample every 20th pixel like Android version
        for (i in pixels.indices step 20) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xff
            val g = (pixel shr 8) and 0xff
            val b = pixel and 0xff

            brightness += (0.299f * r + 0.587f * g + 0.114f * b) / 255f
            sampleCount++
        }
    }

    return if (sampleCount > 0) brightness / sampleCount else 0f
}

private fun adjustBrightness(imageBitmap: ImageBitmap, factor: Float): ImageBitmap {
    val skiaBitmap = imageBitmap.asSkiaBitmap()
    val width = skiaBitmap.width
    val height = skiaBitmap.height

    // Create Skia color matrix for brightness adjustment
    val colorMatrix = SkiaColorMatrix(
        factor, 0f, 0f, 0f, 0f,
        0f, factor, 0f, 0f, 0f,
        0f, 0f, factor, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )

    // Create Skia paint with color filter
    val paint = SkiaPaint().apply {
        colorFilter = SkiaColorFilter.makeMatrix(colorMatrix)
    }

    // Create result Skia bitmap
    val resultBitmap = SkiaBitmap().apply {
        allocN32Pixels(width, height)
    }

    // Draw original bitmap with brightness adjustment using Skia canvas
    val canvas = SkiaCanvas(resultBitmap)
    canvas.drawImage(SkiaImage.makeFromBitmap(skiaBitmap), 0f, 0f, paint)

    // Convert SkiaBitmap to SkiaImage first, then to ComposeImageBitmap
    val resultImage = SkiaImage.makeFromBitmap(resultBitmap)
    return resultImage.toComposeImageBitmap()
}
