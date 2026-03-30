@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
@file:OptIn(BetaInteropApi::class)

package com.chaintech.faceliveness.util

import kotlinx.cinterop.*
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionRecordPermissionDenied
import platform.AVFAudio.AVAudioSessionRecordPermissionGranted
import platform.AVFAudio.AVAudioSessionRecordPermissionUndetermined
import platform.AVFoundation.*
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.*
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceOrientation
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
actual class VideoRecorder {
    private var movieOutput: AVCaptureMovieFileOutput? = null
    private var captureSession: AVCaptureSession? = null
    private var savedVideoPath: String? = null
    private var onResultCallback: ((String) -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null
    private var recordingDelegate: AVCaptureFileOutputRecordingDelegateProtocol? = null

    // Constants for single video management
    private val VIDEO_FILENAME = "kyc_face_liveness_video.mp4"
    private val VIDEO_FOLDER = "Videos"

    @OptIn(BetaInteropApi::class)
    private inner class RecordingDelegate : NSObject(), AVCaptureFileOutputRecordingDelegateProtocol {

        override fun captureOutput(
            output: AVCaptureFileOutput,
            didFinishRecordingToOutputFileAtURL: NSURL,
            fromConnections: List<*>,
            error: NSError?
        ) {
            if (error != null) {
                val errorMessage = error.localizedDescription
                println("❌ iOS Video Recording Error: $errorMessage")

                // Check specific error codes
                when (error.code) {
                    AVErrorDiskFull -> onErrorCallback?.invoke("Storage full")
                    AVErrorSessionWasInterrupted -> onErrorCallback?.invoke("Recording interrupted")
                    AVErrorMaximumDurationReached -> {
                        // This is actually success with max duration reached
                        handleSuccessfulRecording(didFinishRecordingToOutputFileAtURL)
                    }
                    else -> onErrorCallback?.invoke(errorMessage)
                }
            } else {
                handleSuccessfulRecording(didFinishRecordingToOutputFileAtURL)
            }
        }

        private fun handleSuccessfulRecording(fileURL: NSURL) {
            val path = fileURL.path
            if (path != null) {
                savedVideoPath = path
                println("✅ iOS Video saved to: $path")

                // Verify file exists and get size
                val fileManager = NSFileManager.defaultManager
                if (fileManager.fileExistsAtPath(path)) {
                    memScoped {
                        val error = alloc<ObjCObjectVar<NSError?>>()
                        val attributes = fileManager.attributesOfItemAtPath(path, error.ptr)
                        val fileSize = attributes?.get(NSFileSize) as? NSNumber
                        val fileSizeMB = (fileSize?.longValue ?: 0) / (1024 * 1024)
                        println("📹 Video file size: $fileSizeMB MB")
                    }
                }

                onResultCallback?.invoke(path)
            } else {
                onErrorCallback?.invoke("Failed to get video path")
            }
        }

        override fun captureOutput(
            output: AVCaptureFileOutput,
            didStartRecordingToOutputFileAtURL: NSURL,
            fromConnections: List<*>
        ) {
            println("▶️ iOS Video recording started for: $VIDEO_FILENAME")
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    fun setupVideoOutput(session: AVCaptureSession) {
        captureSession = session

        // Begin configuration
        session.beginConfiguration()

        // Create and configure movie output
        val output = AVCaptureMovieFileOutput().apply {
            // Enable movie fragment writing for better recovery if recording is interrupted
            movieFragmentInterval = CMTimeMakeWithSeconds(1.0, preferredTimescale = 600)
        }

        // Check if output can be added
        if (session.canAddOutput(output)) {
            session.addOutput(output)
            movieOutput = output

            // Configure video connection
            output.connectionWithMediaType(AVMediaTypeVideo)?.let { connection ->
                // Enable video stabilization if available
                if (connection.isVideoStabilizationSupported()) {
                    connection.preferredVideoStabilizationMode = AVCaptureVideoStabilizationModeAuto
                }

                // Set video orientation
                if (connection.isVideoOrientationSupported()) {
                    connection.videoOrientation = getCurrentVideoOrientation()
                }
            }

            // Configure audio connection if available
            output.connectionWithMediaType(AVMediaTypeAudio)?.let { _ ->
                println("✅ Audio connection available for video recording")
            }

            println("✅ iOS Movie output added to session")
        } else {
            println("❌ iOS Cannot add movie output to session")
        }

        // Commit configuration
        session.commitConfiguration()
    }

    actual fun startRecording(
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        this.onResultCallback = onResult
        this.onErrorCallback = onError

        val output = movieOutput
        if (output == null) {
            onError("Movie output not initialized. Call setupVideoOutput first.")
            return
        }

        if (output.isRecording()) {
            println("⚠️ iOS Already recording")
            onError("Already recording")
            return
        }

        // Delete existing video and create new file URL
        deleteExistingVideo()

        val fileURL = getVideoFileURL()
        if (fileURL == null) {
            onError("Failed to create output file URL")
            return
        }

        println("▶️ iOS Starting recording to: ${fileURL.path}")

        // Create and store delegate
        recordingDelegate = RecordingDelegate()

        // Start recording
        recordingDelegate?.let {
            output.startRecordingToOutputFileURL(
                outputFileURL = fileURL,
                recordingDelegate = it
            )
        }
    }

    actual fun stopRecording() {
        val output = movieOutput
        if (output == null) {
            println("⚠️ iOS Movie output is null")
            return
        }

        if (!output.isRecording()) {
            println("⚠️ iOS Not recording")
            return
        }

        println("⏹️ iOS Stopping recording")
        output.stopRecording()
    }

    actual fun getVideoUri(): String? {
        // Always return the path to our single video file if it exists
        val videoPath = getVideoFilePath()
        val fileManager = NSFileManager.defaultManager

        return if (fileManager.fileExistsAtPath(videoPath)) {
            savedVideoPath = videoPath
            videoPath
        } else {
            null
        }
    }

    actual fun release() {
        // Stop recording if in progress
        if (movieOutput?.isRecording() == true) {
            stopRecording()
        }

        // Remove output from session
        movieOutput?.let { output ->
            captureSession?.removeOutput(output)
        }

        // Clear all references
        movieOutput = null
        captureSession = null
        savedVideoPath = null
        onResultCallback = null
        onErrorCallback = null
        recordingDelegate = null

        println("🧹 iOS VideoRecorder released")
    }

    /**
     * Delete existing video file if it exists
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun deleteExistingVideo() {
        val videoPath = getVideoFilePath()
        val fileManager = NSFileManager.defaultManager

        if (fileManager.fileExistsAtPath(videoPath)) {
            memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                val success = fileManager.removeItemAtPath(videoPath, error.ptr)

                if (success) {
                    println("🗑️ Deleted existing video: $VIDEO_FILENAME")
                } else {
                    println("❌ Failed to delete existing video: ${error.value?.localizedDescription}")
                }
            }
        }
    }

    /**
     * Get the single video file path
     */
    private fun getVideoFilePath(): String {
        val paths = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        )

        val documentsPath = paths.firstOrNull() as? String ?: ""
        return "$documentsPath/$VIDEO_FOLDER/$VIDEO_FILENAME"
    }

    /**
     * Get the video file URL, creating directory if needed
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun getVideoFileURL(): NSURL? {
        // Get documents directory
        val paths = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        )

        val documentsPath = paths.firstOrNull() as? String
        if (documentsPath == null) {
            println("❌ Cannot access documents directory")
            return null
        }

        // Create videos subdirectory if it doesn't exist
        val videosPath = "$documentsPath/$VIDEO_FOLDER"
        val fileManager = NSFileManager.defaultManager

        if (!fileManager.fileExistsAtPath(videosPath)) {
            memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                val success = fileManager.createDirectoryAtPath(
                    videosPath,
                    withIntermediateDirectories = true,
                    attributes = null,
                    error = error.ptr
                )

                if (!success || error.value != null) {
                    println("❌ Failed to create Videos directory: ${error.value?.localizedDescription}")
                    return null
                }

                println("📁 Created Videos directory")
            }
        }

        // Always use the same filename
        val filePath = "$videosPath/$VIDEO_FILENAME"

        return NSURL.fileURLWithPath(filePath)
    }

    /**
     * Check if a video exists
     */
    fun hasExistingVideo(): Boolean {
        val videoPath = getVideoFilePath()
        return NSFileManager.defaultManager.fileExistsAtPath(videoPath)
    }

    /**
     * Delete the video file
     */
    @OptIn(ExperimentalForeignApi::class)
    fun deleteVideo(): Boolean {
        val videoPath = getVideoFilePath()
        val fileManager = NSFileManager.defaultManager

        if (fileManager.fileExistsAtPath(videoPath)) {
            memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                val success = fileManager.removeItemAtPath(videoPath, error.ptr)

                if (success) {
                    println("✅ Video deleted successfully")
                    savedVideoPath = null
                    return true
                } else {
                    println("❌ Failed to delete video: ${error.value?.localizedDescription}")
                    return false
                }
            }
        }

        return true // No file to delete
    }

    private fun getCurrentVideoOrientation(): AVCaptureVideoOrientation {
        return when (UIDevice.currentDevice.orientation) {
            UIDeviceOrientation.UIDeviceOrientationLandscapeLeft ->
                AVCaptureVideoOrientationLandscapeRight
            UIDeviceOrientation.UIDeviceOrientationLandscapeRight ->
                AVCaptureVideoOrientationLandscapeLeft
            UIDeviceOrientation.UIDeviceOrientationPortraitUpsideDown ->
                AVCaptureVideoOrientationPortraitUpsideDown
            else ->
                AVCaptureVideoOrientationPortrait
        }
    }
}

// Extension to check if audio permission is granted
@OptIn(ExperimentalForeignApi::class)
fun checkAudioPermissionForVideoRecording(completion: (Boolean) -> Unit) {
    val audioSession = AVAudioSession.sharedInstance()

    when (audioSession.recordPermission) {
        AVAudioSessionRecordPermissionGranted -> {
            completion(true)
        }
        AVAudioSessionRecordPermissionDenied -> {
            completion(false)
        }
        AVAudioSessionRecordPermissionUndetermined -> {
            audioSession.requestRecordPermission { granted ->
                completion(granted)
            }
        }
        else -> {
            completion(false)
        }
    }
}

