package com.chaintech.documentcontroller.scanner

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.refTo
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import org.jetbrains.skia.Image.Companion.makeFromEncoded
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureExposureModeContinuousAutoExposure
import platform.AVFoundation.AVCaptureFocusModeContinuousAutoFocus
import platform.AVFoundation.AVCapturePhoto
import platform.AVFoundation.AVCapturePhotoCaptureDelegateProtocol
import platform.AVFoundation.AVCapturePhotoOutput
import platform.AVFoundation.AVCapturePhotoSettings
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetPhoto
import platform.AVFoundation.AVCaptureVideoOrientationPortrait
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVVideoCodecKey
import platform.AVFoundation.AVVideoCodecTypeJPEG
import platform.AVFoundation.exposureMode
import platform.AVFoundation.fileDataRepresentation
import platform.AVFoundation.focusMode
import platform.AVFoundation.isExposureModeSupported
import platform.AVFoundation.isFocusModeSupported
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageOrientation
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIView
import platform.darwin.NSObject
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
class CameraPreviewView : UIView(CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    private val captureSession = AVCaptureSession()
    private val photoOutput = AVCapturePhotoOutput()
    private val previewLayer = AVCaptureVideoPreviewLayer(session = captureSession)
    private var videoDeviceInput: AVCaptureDeviceInput? = null

    // Add a flag to track configuration status
    private var isConfigured = false
    private var isConfiguring = false

    // Create a coroutine scope for camera operations
    private val cameraScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        setupUI()
        setupCamera()
    }

    private fun setupUI() {
        previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
        layer.addSublayer(previewLayer)
    }

    private fun setupCamera() {
        cameraScope.launch {
            configureCaptureSession()
        }
    }

    private suspend fun configureCaptureSession() {
        if (isConfiguring) return
        isConfiguring = true

        try {
            captureSession.beginConfiguration()

            captureSession.sessionPreset = AVCaptureSessionPresetPhoto

            val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)

            device?.let { videoDevice ->
                // Lock device for configuration
                try {
                    videoDevice.lockForConfiguration(null)

                    // Set focus mode if available
                    if (videoDevice.isFocusModeSupported(AVCaptureFocusModeContinuousAutoFocus)) {
                        videoDevice.focusMode = AVCaptureFocusModeContinuousAutoFocus
                    }

                    // Set exposure mode if available
                    if (videoDevice.isExposureModeSupported(AVCaptureExposureModeContinuousAutoExposure)) {
                        videoDevice.exposureMode = AVCaptureExposureModeContinuousAutoExposure
                    }

                    videoDevice.unlockForConfiguration()
                } catch (e: Exception) {
                    println("Error configuring device: ${e.message}")
                }

                val input = AVCaptureDeviceInput.deviceInputWithDevice(videoDevice, null)

                if (input != null && captureSession.canAddInput(input)) {
                    captureSession.addInput(input)
                    videoDeviceInput = input
                }
            }

            if (captureSession.canAddOutput(photoOutput)) {
                captureSession.addOutput(photoOutput)
                // Configure photo output for portrait orientation
                configurePhotoOutputConnection()
            }

            captureSession.commitConfiguration()

            // Mark configuration as complete
            isConfigured = true

        } finally {
            isConfiguring = false
        }

        // Auto-start session after configuration
        startSessionInternal()
    }

    private fun configurePhotoOutputConnection() {
        // Get the connection between the photo output and the video preview
        val connection = photoOutput.connectionWithMediaType(AVMediaTypeVideo)

        connection?.let { photoConnection ->
            // Lock the orientation to portrait
            if (photoConnection.isVideoOrientationSupported()) {
                photoConnection.videoOrientation = AVCaptureVideoOrientationPortrait
            }

            // Disable video mirroring for back camera
            if (photoConnection.isVideoMirroringSupported()) {
                photoConnection.videoMirrored = false
            }
        }

        // Also configure the preview layer connection
        previewLayer.connection?.let { previewConnection ->
            if (previewConnection.isVideoOrientationSupported()) {
                previewConnection.videoOrientation = AVCaptureVideoOrientationPortrait
            }
        }
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        previewLayer.frame = bounds

        // Ensure orientation stays portrait when layout changes
        updateOrientation()
    }

    private fun updateOrientation() {
        // Update preview layer orientation
        previewLayer.connection?.let { connection ->
            if (connection.isVideoOrientationSupported()) {
                connection.videoOrientation = AVCaptureVideoOrientationPortrait
            }
        }

        // Update photo output orientation
        photoOutput.connectionWithMediaType(AVMediaTypeVideo)?.let { connection ->
            if (connection.isVideoOrientationSupported()) {
                connection.videoOrientation = AVCaptureVideoOrientationPortrait
            }
        }
    }

    private suspend fun startSessionInternal() {
        if (!captureSession.running && isConfigured && !isConfiguring) {
            captureSession.startRunning()

            // Ensure orientation is set after starting
            withContext(Dispatchers.Main) {
                updateOrientation()
            }
        }
    }

    fun startSession() {
        cameraScope.launch {
            // Wait for configuration to complete if it's in progress
            while (isConfiguring) {
                delay(50)
            }

            // Only start if configured
            if (isConfigured) {
                startSessionInternal()
            } else {
                // If not configured yet, configure first
                configureCaptureSession()
            }
        }
    }

    fun stopSession() {
        cameraScope.launch {
            // Wait for any ongoing configuration to complete
            while (isConfiguring) {
                delay(50)
            }

            if (captureSession.running) {
                captureSession.stopRunning()
            }
        }
    }

    fun cleanup() {
        cameraScope.cancel()
        stopSession()
    }

    fun takePhoto(onPhotoCaptured: (ImageBitmap?) -> Unit) {
        // Ensure session is configured before taking photo
        if (!isConfigured) {
            println("Camera not configured yet")
            onPhotoCaptured(null)
            return
        }

        val settings = AVCapturePhotoSettings.photoSettings()

        // Only enable stabilization if supported
        if (photoOutput.isStillImageStabilizationSupported()) {
            settings.autoStillImageStabilizationEnabled = true
        }

        // Ensure orientation is set before capture
        photoOutput.connectionWithMediaType(AVMediaTypeVideo)?.let { connection ->
            if (connection.isVideoOrientationSupported()) {
                connection.videoOrientation = AVCaptureVideoOrientationPortrait
            }
        }

        val delegate = PhotoCaptureDelegate(
            onResult = {
                onPhotoCaptured(it)
            }
        )

        cameraScope.launch {
            // Wait for configuration if needed
            while (isConfiguring) {
                delay(50)
            }

            if (isConfigured && captureSession.running) {
                photoOutput.capturePhotoWithSettings(settings, delegate)
            } else {
                withContext(Dispatchers.Main) {
                    onPhotoCaptured(null)
                }
            }
        }
    }
}

private class PhotoCaptureDelegate(
    private val onResult: (ImageBitmap?) -> Unit
) : NSObject(), AVCapturePhotoCaptureDelegateProtocol {
    override fun captureOutput(
        output: AVCapturePhotoOutput,
        didFinishProcessingPhoto: AVCapturePhoto,
        error: NSError?
    ) {
        if (error != null) {
            println("Photo capture error: ${error.localizedDescription}")
            onResult(null)
            return
        }
        val data = didFinishProcessingPhoto.processToPortraitPng()
        onResult(data)
    }
}

// Extension to process photo and ensure portrait orientation
private fun AVCapturePhoto.processToPortraitPng(): ImageBitmap? {
    val imageData = this.fileDataRepresentation() ?: return null
    val uiImage = UIImage(data = imageData)

    // Force portrait orientation if needed
    val orientedImage = if (uiImage.imageOrientation != UIImageOrientation.UIImageOrientationUp) {
        println("Force portrait orientation --> Not Up")
        uiImage.normalizeOrientation()
    } else {
        println("Force portrait orientation --> It's Up")
        uiImage
    }

    // Convert to PNG
    val pngData = UIImagePNGRepresentation(orientedImage) ?: return null
    return pngData.toByteArray().decodeToImageBitmap()
}

// Extension to normalize image orientation to portrait
@OptIn(ExperimentalForeignApi::class)
private fun UIImage.normalizeOrientation(): UIImage {
    if (this.imageOrientation == UIImageOrientation.UIImageOrientationUp) return this

    val width = this.size.useContents { width }
    val height = this.size.useContents { height }

    UIGraphicsBeginImageContextWithOptions(
        CGSizeMake(width, height),
        false,
        this.scale
    )

    this.drawInRect(CGRectMake(0.0, 0.0, width, height))
    val normalizedImage = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()

    return normalizedImage ?: this
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray =
    ByteArray(length.toInt()).also { array ->
        array.usePinned {
            memcpy(it.addressOf(0), bytes, length)
        }
    }
