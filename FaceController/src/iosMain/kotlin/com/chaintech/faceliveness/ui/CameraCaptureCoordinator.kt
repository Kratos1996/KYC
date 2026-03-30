package com.chaintech.faceliveness.ui

import androidx.compose.ui.graphics.ImageBitmap
import com.chaintech.faceliveness.util.toImageBitmap
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.AVFoundation.*
import platform.CoreFoundation.CFRelease
import platform.CoreGraphics.*
import platform.CoreImage.*
import platform.CoreMedia.*
import platform.CoreVideo.*
import platform.Foundation.*
import platform.QuartzCore.CALayer
import platform.UIKit.*
import platform.darwin.*
import kotlin.concurrent.Volatile

@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
class CameraCaptureCoordinator(
    val camera: AVCaptureDevice,
    val onFrameCaptured: (ImageBitmap?) -> Unit
) : NSObject(), AVCaptureVideoDataOutputSampleBufferDelegateProtocol {

    private var previewLayer: AVCaptureVideoPreviewLayer? = null
    private var videoOutput: AVCaptureVideoDataOutput? = null
    private var videoQueue: dispatch_queue_t? = null

    @Volatile
    private var isCapturingFrames = false
    private var sessionQueue: dispatch_queue_t? = null
    private val ciContext: CIContext by lazy {
        CIContext.contextWithOptions(
            mapOf(
                kCIContextUseSoftwareRenderer to false,
                kCIContextPriorityRequestLow to true
            )
        )
    }

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    lateinit var captureSession: AVCaptureSession

    fun prepare(layer: CALayer) {
        captureSession = AVCaptureSession()

        // create the session queue once
        if (sessionQueue == null) {
            sessionQueue = dispatch_queue_create("com.camera.sessionQueue", null)
        }

        // run whole configuration on the session queue
        dispatch_async(sessionQueue) {
            autoreleasepool {
                captureSession.beginConfiguration()

                // Configure session preset
                when {
                    captureSession.canSetSessionPreset(AVCaptureSessionPreset640x480) -> {
                        captureSession.sessionPreset = AVCaptureSessionPreset640x480
                    }

                    captureSession.canSetSessionPreset(AVCaptureSessionPresetLow) -> {
                        captureSession.sessionPreset = AVCaptureSessionPresetLow
                    }
                }

                // Remove existing inputs safely
                val inputs = captureSession.inputs
                inputs.forEach { input ->
                    (input as? AVCaptureInput)?.let {
                        captureSession.removeInput(it)
                    }
                }

                // Configure camera
                configureCameraForVideo()

                val videoInput = createVideoInput()
                if (videoInput != null && captureSession.canAddInput(videoInput)) {
                    captureSession.addInput(videoInput)
                } else {
                    println("Failed to add video input")
                    captureSession.commitConfiguration()
                    return@autoreleasepool
                }

                setupVideoDataOutput() // this only configures videoOutput, adds output to session

                captureSession.commitConfiguration()

                autoreleasepool {
                    if (!captureSession.isRunning()) {
                        captureSession.startRunning()
                    }

                    dispatch_async(dispatch_get_main_queue()) {
                        setCurrentOrientation(newOrientation = UIDeviceOrientation.UIDeviceOrientationPortrait)
                    }
                }

                // The UI work can still be dispatched to the main thread.
                dispatch_async(dispatch_get_main_queue()) {
                    autoreleasepool {
                        setupPreviewLayer(layer)
                    }
                }
            }
        }
    }

    private fun setupPreviewLayer(layer: CALayer) {
        autoreleasepool {
            previewLayer = AVCaptureVideoPreviewLayer(session = captureSession).also {
                it.frame = layer.bounds
                it.videoGravity = AVLayerVideoGravityResizeAspectFill
                layer.addSublayer(it)
            }
        }
    }

    private fun createVideoInput(): AVCaptureDeviceInput? {
        return autoreleasepool {
            memScoped {
                val error: ObjCObjectVar<NSError?> = alloc()
                val input = AVCaptureDeviceInput(device = camera, error = error.ptr)

                if (error.value != null) {
                    println("Video input error: ${error.value?.localizedDescription}")
                    null
                } else {
                    input
                }
            }
        }
    }

    private fun configureCameraForVideo() {
        autoreleasepool {
            memScoped {
                val errorPtr: ObjCObjectVar<NSError?> = alloc()
                if (camera.lockForConfiguration(errorPtr.ptr)) {
                    // Configure focus
                    if (camera.isFocusModeSupported(AVCaptureFocusModeContinuousAutoFocus)) {
                        camera.focusMode = AVCaptureFocusModeContinuousAutoFocus
                    }

                    // Configure exposure
                    if (camera.isExposureModeSupported(AVCaptureExposureModeContinuousAutoExposure)) {
                        camera.exposureMode = AVCaptureExposureModeContinuousAutoExposure
                    }

                    camera.unlockForConfiguration()
                }
            }
        }
    }

    private fun setupVideoDataOutput() {
        autoreleasepool {
            videoOutput = AVCaptureVideoDataOutput().apply {
                // Configure pixel format
                @Suppress("UNCHECKED_CAST")
                val videoSettings = mapOf(
                    kCVPixelBufferPixelFormatTypeKey to NSNumber.numberWithInt(
                        kCVPixelFormatType_32BGRA.toInt()
                    )
                ) as Map<Any?, Any?>

                this.videoSettings = videoSettings

                this.alwaysDiscardsLateVideoFrames = true
            }

            // Create queue
            val queueLabel = "com.camera.videoQueue"
            videoQueue = dispatch_queue_create(queueLabel, null)

            // Set delegate
            videoOutput?.setSampleBufferDelegate(this, queue = videoQueue)

            // Add to session
            videoOutput?.let { output ->
                if (captureSession.canAddOutput(output)) {
                    captureSession.addOutput(output)
                }
            }
        }
    }

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputSampleBuffer: CMSampleBufferRef?,
        fromConnection: AVCaptureConnection
    ) {
        if (!isCapturingFrames) return

        autoreleasepool {
            didOutputSampleBuffer?.let { sampleBuffer ->
                val imageBitmap = sampleBufferToUIImage(sampleBuffer)?.toImageBitmap()

                dispatch_async(dispatch_get_main_queue()) {
                    autoreleasepool {
                        onFrameCaptured(imageBitmap)
                    }
                }
            }
        }
    }

    private fun sampleBufferToUIImage(sampleBuffer: CMSampleBufferRef): UIImage? {
        return autoreleasepool {
            val imageBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) ?: return@autoreleasepool null

            CVPixelBufferLockBaseAddress(imageBuffer, kCVPixelBufferLock_ReadOnly)

            try {
                val width = CVPixelBufferGetWidth(imageBuffer)
                val height = CVPixelBufferGetHeight(imageBuffer)

                val ciImage = CIImage.imageWithCVPixelBuffer(imageBuffer)

                val cgImage = ciContext.createCGImage(
                    ciImage,
                    fromRect = CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble())
                ) ?: return@autoreleasepool null

                val uiImage = UIImage.imageWithCGImage(cgImage)

                cgImageRelease(cgImage)

                return@autoreleasepool uiImage
            } finally {
                CVPixelBufferUnlockBaseAddress(imageBuffer, kCVPixelBufferLock_ReadOnly)
            }
        }
    }

    // Start capturing frames
    fun startCapturingFrames() {
        isCapturingFrames = true
    }

    // Stop capturing frames
    fun stopCapturingFrames() {
        isCapturingFrames = false
    }

    fun setCurrentOrientation(newOrientation: UIDeviceOrientation) {
        val videoOrientation = when (newOrientation) {
            UIDeviceOrientation.UIDeviceOrientationLandscapeLeft ->
                AVCaptureVideoOrientationLandscapeRight
            UIDeviceOrientation.UIDeviceOrientationLandscapeRight ->
                AVCaptureVideoOrientationLandscapeLeft
            UIDeviceOrientation.UIDeviceOrientationPortrait ->
                AVCaptureVideoOrientationPortrait
            UIDeviceOrientation.UIDeviceOrientationPortraitUpsideDown ->
                AVCaptureVideoOrientationPortraitUpsideDown
            else ->
                AVCaptureVideoOrientationPortrait
        }

        previewLayer?.connection?.videoOrientation = videoOrientation

        // Update video output connection orientation
        videoOutput?.connectionWithMediaType(AVMediaTypeVideo)?.let { connection ->
            if (connection.isVideoOrientationSupported()) {
                connection.videoOrientation = videoOrientation
            }

            if (connection.isVideoMirroringSupported()) {
                connection.setVideoMirrored(false)
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    fun setFrame(rect: CValue<CGRect>) {
        previewLayer?.setFrame(rect)
    }

    // Check if camera is running
    fun isCameraRunning(): Boolean = captureSession.isRunning()

    // Pause camera session
    fun pauseSession() {
        if (!::captureSession.isInitialized) {
            println("captureSession not ready yet, skipping pause")
            return
        }
        dispatch_async(sessionQueue) {
            if (captureSession.isRunning()) {
                captureSession.stopRunning()
            }
        }
    }

    fun resumeSession() {
        if (!::captureSession.isInitialized) {
            println("captureSession not ready yet, skipping resume")
            return
        }

        dispatch_async(sessionQueue) {
            if (!captureSession.isRunning()) {
                captureSession.startRunning()
            }
        }
    }

    // Clean up resources
    fun stopSession() {
        // If the session was never prepared, there's nothing to stop.
        if (!::captureSession.isInitialized || sessionQueue == null) {
            println("CameraCaptureCoordinator: stopSession called before prepare, skipping.")
            return
        }

        // stop capture & cleanup on sessionQueue to keep operations serialized
        if (sessionQueue == null) {
            sessionQueue = dispatch_queue_create("com.camera.sessionQueue", null)
        }

        dispatch_async(sessionQueue) {
            isCapturingFrames = false

            if (captureSession.isRunning()) {
                captureSession.stopRunning()
            }

            // Remove outputs/inputs safely inside configuration
            captureSession.beginConfiguration()
            videoOutput?.let { output ->
                output.setSampleBufferDelegate(null, queue = null)
                captureSession.removeOutput(output)
            }

            // Also remove inputs
            val inputs = captureSession.inputs
            inputs.forEach { input ->
                (input as? AVCaptureInput)?.let {
                    captureSession.removeInput(it)
                }
            }

            captureSession.commitConfiguration()

            // UI cleanup on main thread
            dispatch_async(dispatch_get_main_queue()) {
                previewLayer?.removeFromSuperlayer()
                previewLayer = null
            }

            videoOutput = null
            videoQueue = null
        }
    }
}

// External function to release CGImage if needed
@OptIn(ExperimentalForeignApi::class)
private fun cgImageRelease(image: CGImageRef?) {
    image?.let {
        CFRelease(it.reinterpret())
    }
}