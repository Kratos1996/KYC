package com.chaintech.faceliveness.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import com.chaintech.faceliveness.util.DetectionState
import com.chaintech.faceliveness.util.FaceLandmarkerHelper
import com.chaintech.faceliveness.util.FacePoseCalculator
import com.chaintech.faceliveness.util.ImageCaptureHelper
import com.chaintech.faceliveness.util.VideoRecorder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaintech.faceliveness.base.FaceConfig
import com.chaintech.faceliveness.base.FaceMoveDetection
import com.chaintech.faceliveness.base.FaceMoveDiagonalDetection
import com.chaintech.faceliveness.base.FaceTask
import com.chaintech.faceliveness.util.BorderColor
import com.chaintech.faceliveness.util.BorderState
import com.chaintech.faceliveness.util.FacePosition
import com.chaintech.facialAttributeDetection.FacialAttributeDetectionSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FaceDetectionViewModel(
    private val videoRecorder: VideoRecorder,
    private val imageCaptureHelper: ImageCaptureHelper,
    private val faceLandmarkerHelper: FaceLandmarkerHelper,
    private val facePoseCalculator: FacePoseCalculator,
    private val facialAttributeDetectionSession: FacialAttributeDetectionSession
) : ViewModel() {

    companion object {
        private const val TAG = "FaceDetectionViewModel"
    }

    // ===== STATE FLOWS =====
    private val _brightness = MutableStateFlow(0.5f)
    val brightness: StateFlow<Float> = _brightness.asStateFlow()

    private val _showBrightnessWarning = MutableStateFlow(false)
    val showBrightnessWarning: StateFlow<Boolean> = _showBrightnessWarning.asStateFlow()

    private val _detectionState = MutableStateFlow<DetectionState>(DetectionState.Idle)
    val detectionState: StateFlow<DetectionState> = _detectionState.asStateFlow()

    private val _processDone = MutableStateFlow(false)
    val processDone: StateFlow<Boolean> = _processDone.asStateFlow()

    private val _finalSelfieBitmap = MutableStateFlow<Any?>(null)
    val finalSelfieBitmap: StateFlow<Any?> = _finalSelfieBitmap.asStateFlow()

    private val _finalSelfieVideo = MutableStateFlow<String?>(null)
    val finalSelfieVideo: StateFlow<String?> = _finalSelfieVideo.asStateFlow()

    private val _borderState = MutableStateFlow(BorderState(color = BorderColor.RED))
    val borderState: StateFlow<BorderState> = _borderState.asStateFlow()

    private val _message = MutableStateFlow("Position your face in the circle")
    val message: StateFlow<String> = _message.asStateFlow()

    // ===== PRIVATE STATE =====
    private var currentBitmap: Any? = null
    private var blinkCount = 0
    private var eyesClosedFrames = 0
    private var wasEyesClosed = false
    private var holdJob: Job? = null
    private var frameCount = 0L
    private var noFaceFrameCount = 0
    private var isRecording = false
    private var moveFrameCount = 0
    private var baseMsgOfEyeBlink = "Start Blink your Eyes"

    // ===== CONFIGURATION =====
    private val _isDetecting = MutableStateFlow(false)
    val isDetecting = _isDetecting.asStateFlow()

    val faceConfigValue = mutableStateOf<FaceConfig>(FaceConfig())

    // ===== TASK TRACKING VARIABLES =====
    private var currentTaskIndex = 0
    private var currentMoveIndex = 0
    private var currentDiagonalMoveIndex = 0

    // ✅ Separate tracking for regular and diagonal moves
    private var completedMoves = mutableListOf<FaceMoveDetection>()
    private var completedDiagonalMoves = mutableListOf<FaceMoveDiagonalDetection>()

    // ===== INITIALIZATION =====
    fun initializeFaceDetection() {
        println("$TAG: ════════════════════════════════════════")
        println("$TAG: 🚀 Initializing face detection")
        println("$TAG: Tasks: ${faceConfigValue.value.listOfFaceTask}")
        println("$TAG: Regular moves: ${faceConfigValue.value.listOfFaceDetection}")
        println("$TAG: Diagonal moves: ${faceConfigValue.value.listOfFaceDiagonalDetection}")
        println("$TAG: ════════════════════════════════════════")

        viewModelScope.launch {
            delay(100)
            faceLandmarkerHelper.initialize { error ->
                _detectionState.value = DetectionState.Error(error)
                _message.value = "Error: $error"
                println("$TAG: ❌ Initialization error: $error")
            }
            delay(100)
        }

        viewModelScope.launch {
            delay(100)
            facialAttributeDetectionSession.initialize()
        }

        viewModelScope.launch {
            faceLandmarkerHelper.resultsFlow.collect { result ->
                if (result == null) {
                    noFaceFrameCount++
                    if (noFaceFrameCount > faceConfigValue.value.requiredFaceFrames) {
                        handleNoFaceDetectedWithBrightness(_brightness.value)
                    }
                } else {
                    noFaceFrameCount = 0
                    processResult(result)
                }
            }
        }
    }

    // ===== BRIGHTNESS MANAGEMENT =====
    fun updateBrightness(level: Float) {
        _brightness.value = level
        _showBrightnessWarning.value = level < faceConfigValue.value.brightnessDarkValue.value ||
                level > faceConfigValue.value.brightnessBrightValue.value
    }

    fun getBrightnessStatus(): String {
        val level = _brightness.value
        return when {
            level < faceConfigValue.value.brightnessTooDarkValue.value -> "Too Dark"
            level < faceConfigValue.value.brightnessDarkValue.value -> "Low Light"
            level < faceConfigValue.value.brightnessBrightValue.value -> "Good Light"
            level < faceConfigValue.value.brightnessTooBrightValue.value -> "Bright"
            else -> "Too Bright"
        }
    }

    fun getBrightnessPercentage(): Int = (_brightness.value * 100).toInt()

    private fun addBrightnessWarningIfNeeded(baseMessage: String, brightness: Float): String {
        val warning = when {
            brightness < faceConfigValue.value.brightnessTooDarkValue.value -> " | 🌑 Too dark"
            brightness < faceConfigValue.value.brightnessDarkValue.value -> " | 🌓 Low light"
            brightness > faceConfigValue.value.brightnessTooBrightValue.value -> " | 💡 Too bright"
            brightness > faceConfigValue.value.brightnessBrightValue.value -> " | ☀️ Bright"
            else -> ""
        }
        return baseMessage + warning
    }

    // ===== BITMAP MANAGEMENT =====
    fun setCurrentBitmap(bitmap: ImageBitmap) {
        currentBitmap = bitmap
    }

    fun getCurrentBitmap(): Any? = currentBitmap

    // ===== FRAME PROCESSING =====
    fun processFrame(bitmap: ImageBitmap) {
        faceLandmarkerHelper.detectAsync(bitmap, frameCount++)
    }

    // ===== MAIN PROCESS RESULT =====
    private suspend fun processResult(position: FacePosition) {
        val currentBrightness = _brightness.value
        if (!position.isFaceDetected || !position.faceInCircle) {
            handleNoFaceDetectedWithBrightness(currentBrightness)
            return
        }

        // Check if face is too far or not
        if (position.isFaceTooFar) {
            handleFaceTooFar(position.faceSize)
            return
        }

        // Check if face is too close or not
        if (position.isFaceTooClose) {
            handleFaceTooClose(position.faceSize)
            return
        }

        // Check FacialAttributes like Mask, Glasses, SunGlasses etc...
        val currentImageBitmap = (getCurrentBitmap() as? ImageBitmap) ?: return
        if (isFaceOcclusion(currentImageBitmap)) {
            return
        }

        // Face is valid - start recording if not already
        if (!isRecording) {
            startVideoRecording()
        }

        if (faceConfigValue.value.isBrightnessControllerRequired) {
            if (currentBrightness >= faceConfigValue.value.brightnessConfigValue.value &&
                currentBrightness < faceConfigValue.value.brightnessTooBrightValue.value) {
                processConfigurableTasks(position, currentBrightness)
            } else {
                handleNoFaceDetectedWithBrightness(currentBrightness)
            }
        } else {
            processConfigurableTasks(position, currentBrightness)
        }
    }

    // ===== Facial Attributes DETECTION =====
    private suspend fun isFaceOcclusion(imageBitmap: ImageBitmap): Boolean {
        val facialAttributesResult =
            facialAttributeDetectionSession.detectFacialOcclusion(imageBitmap) ?: return false

        when {
            (facialAttributesResult.glasses.prediction) -> {
                handleFacialOcclusion("Glasses")
                return true
            }
            (facialAttributesResult.mask.prediction) -> {
                handleFacialOcclusion("Mask")
                return true
            }
            (facialAttributesResult.sunglasses.prediction) -> {
                handleFacialOcclusion("Sunglasses")
                return true
            }
        }

        return false
    }

    private fun handleFacialOcclusion(reason: String) {
        _borderState.value = BorderState(color = BorderColor.RED)
        _message.value = "Face is cover with $reason, please uncover your face"

        if (isRecording) {
            stopVideoRecording()
        }

        resetDetection()
    }

    // ===== CONFIGURABLE TASKS PROCESSING =====
    private fun processConfigurableTasks(position: FacePosition, brightness: Float) {
        if (currentTaskIndex >= faceConfigValue.value.listOfFaceTask.size) {
            return
        }

        when (faceConfigValue.value.listOfFaceTask[currentTaskIndex]) {
            FaceTask.FACE_ROTATION -> {
                processFaceRotationTask(position, brightness)
            }
            FaceTask.FACE_ROTATION_DIAGONAL -> {
                processFaceRotationDiagonalTask(position, brightness)
            }
            FaceTask.EYE_BLINK -> {
                detectBlinks(position, brightness)
            }
            FaceTask.SELFIE_CAPTURE -> {
                processSelfieTask()
            }
        }
    }

    // ✅ NEW: Handle face too far
    private fun handleFaceTooFar(faceSize: Float) {
        _borderState.value = BorderState(color = BorderColor.RED)
        val percentage = (faceSize * 100).toInt()
        _message.value = "📏 Move closer to camera"

        if (isRecording) {
            stopVideoRecording()
        }

        if (_detectionState.value !is DetectionState.Completed) {
            resetDetection()
        }
    }

    // ✅ NEW: Handle face too close
    private fun handleFaceTooClose(faceSize: Float) {
        _borderState.value = BorderState(color = BorderColor.RED)
        val percentage = (faceSize * 100).toInt()
        _message.value = "📏 Move away from camera"

        if (isRecording) {
            stopVideoRecording()
        }

        if (_detectionState.value !is DetectionState.Completed) {
            resetDetection()
        }
    }

    // ===== REGULAR FACE ROTATION TASK (4 Cardinal Directions) =====
    private fun processFaceRotationTask(position: FacePosition, brightness: Float) {
        val moveList = faceConfigValue.value.listOfFaceDetection

        if (moveList.isEmpty()) {
            println("$TAG: ⏭️ Regular rotation not configured, skipping")
            currentTaskIndex++
            return
        }

        if (currentMoveIndex >= moveList.size) {
            println("$TAG: ✅ All regular rotation moves completed")
            currentTaskIndex++
            currentMoveIndex = 0
            _message.value = "✅ Head rotation completed!"
            return
        }

        val currentMove = moveList[currentMoveIndex]

        // ✅ Check ONLY cardinal directions (no diagonals here)
        val isCorrectMove = when (currentMove) {
            FaceMoveDetection.MOVE_LEFT -> position.isMovedLeft
            FaceMoveDetection.MOVE_RIGHT -> position.isMovedRight
            FaceMoveDetection.MOVE_TOP -> position.isMovedTop
            FaceMoveDetection.MOVE_BOTTOM -> position.isMovedBottom
        }

        if (isCorrectMove) {
            moveFrameCount++
            val moveText = getMoveText(currentMove)

            _message.value = addBrightnessWarningIfNeeded(
                "Hold $moveText... ($moveFrameCount/${faceConfigValue.value.requiredFaceFrames})",
                brightness
            )

            updateBorderWithBothMoves()

            if (moveFrameCount >= faceConfigValue.value.requiredFaceFrames) {
                completedMoves.add(currentMove)
                moveFrameCount = 0
                currentMoveIndex++

                println("$TAG: ✅ Regular move completed: $currentMove")

                updateBorderWithBothMoves()

                if (currentMoveIndex >= moveList.size) {
                    currentTaskIndex++
                    _message.value = "✅ Perfect! All rotations completed"
                } else {
                    val nextMove = moveList[currentMoveIndex]
                    _message.value = "✅ Great! Now ${getMoveText(nextMove)}"
                }
            }
        } else {
            moveFrameCount = 0
            _message.value = addBrightnessWarningIfNeeded("Move your head ${getMoveText(currentMove)}", brightness)
            updateBorderWithBothMoves()
        }
    }

    // ===== DIAGONAL FACE ROTATION TASK (4 Diagonal Directions) =====
    private fun processFaceRotationDiagonalTask(position: FacePosition, brightness: Float) {
        val moveList = faceConfigValue.value.listOfFaceDiagonalDetection

        if (moveList.isEmpty()) {
            println("$TAG: ⏭️ Diagonal rotation not configured, skipping")
            currentTaskIndex++
            return
        }

        if (currentDiagonalMoveIndex >= moveList.size) {
            println("$TAG: ✅ All diagonal rotation moves completed")
            currentTaskIndex++
            currentDiagonalMoveIndex = 0
            _message.value = "✅ Diagonal rotation completed!"
            return
        }

        val currentMove = moveList[currentDiagonalMoveIndex]

        // ✅ Check ONLY diagonal directions
        val isCorrectMove = when (currentMove) {
            FaceMoveDiagonalDetection.MOVE_TOP_LEFT -> position.isMovedTopLeft
            FaceMoveDiagonalDetection.MOVE_TOP_RIGHT -> position.isMovedTopRight
            FaceMoveDiagonalDetection.MOVE_BOTTOM_LEFT -> position.isMovedBottomLeft
            FaceMoveDiagonalDetection.MOVE_BOTTOM_RIGHT -> position.isMovedBottomRight
        }

        if (isCorrectMove) {
            moveFrameCount++
            val moveText = getDiagonalMoveText(currentMove)

            _message.value = addBrightnessWarningIfNeeded(
                "Hold $moveText... ($moveFrameCount/${faceConfigValue.value.requiredFaceFrames})",
                brightness
            )

            updateBorderWithBothMoves()

            if (moveFrameCount >= faceConfigValue.value.requiredFaceFrames) {
                completedDiagonalMoves.add(currentMove)
                moveFrameCount = 0
                currentDiagonalMoveIndex++

                println("$TAG: ✅ Diagonal move completed: $currentMove")

                updateBorderWithBothMoves()

                if (currentDiagonalMoveIndex >= moveList.size) {
                    currentTaskIndex++
                    _message.value = "✅ Perfect! All diagonal rotations completed"
                } else {
                    val nextMove = moveList[currentDiagonalMoveIndex]
                    _message.value = "✅ Great! Now ${getDiagonalMoveText(nextMove)}"
                }
            }
        } else {
            moveFrameCount = 0
            _message.value = addBrightnessWarningIfNeeded("Move your head ${getDiagonalMoveText(currentMove)}", brightness)
            updateBorderWithBothMoves()
        }
    }

    // ===== GET MOVE TEXT =====
    private fun getMoveText(move: FaceMoveDetection): String {
        return when (move) {
            FaceMoveDetection.MOVE_LEFT -> "LEFT"
            FaceMoveDetection.MOVE_RIGHT -> "RIGHT"
            FaceMoveDetection.MOVE_TOP -> "UP"
            FaceMoveDetection.MOVE_BOTTOM -> "DOWN"
        }
    }

    private fun getDiagonalMoveText(move: FaceMoveDiagonalDetection): String {
        return when (move) {
            FaceMoveDiagonalDetection.MOVE_TOP_LEFT -> "TOP-LEFT"
            FaceMoveDiagonalDetection.MOVE_TOP_RIGHT -> "TOP-RIGHT"
            FaceMoveDiagonalDetection.MOVE_BOTTOM_LEFT -> "BOTTOM-LEFT"
            FaceMoveDiagonalDetection.MOVE_BOTTOM_RIGHT -> "BOTTOM-RIGHT"
        }
    }

    // ===== UPDATE BORDER WITH BOTH MOVE TYPES =====
    private fun updateBorderWithBothMoves() {
        _borderState.value = BorderState(
            color = BorderColor.LIGHT_GREEN,
            completedMoves = completedMoves,
            completedDiagonalMoves = completedDiagonalMoves,
            totalMoves = faceConfigValue.value.listOfFaceDetection,
            totalDiagonalMoves = faceConfigValue.value.listOfFaceDiagonalDetection
        )
    }

    // ===== EYE BLINK TASK =====
    private fun detectBlinks(position: FacePosition, brightness: Float) {
        if (position.isFaceDetected) {
            updateBorderWithBothMoves()
            _message.value = addBrightnessWarningIfNeeded(baseMsgOfEyeBlink, brightness)
        }

        val bothEyesClosed = position.isLeftEyeClosed && position.isRightEyeClosed

        if (bothEyesClosed) {
            eyesClosedFrames++
        } else {
            if (eyesClosedFrames >= faceConfigValue.value.requiredCloseFramesForConfirmation && wasEyesClosed) {
                blinkCount++
                _detectionState.value = DetectionState.BlinkDetection

                baseMsgOfEyeBlink = "👁️ Blink $blinkCount/${faceConfigValue.value.totalEyeCount} - Keep going!"
                _message.value = addBrightnessWarningIfNeeded(baseMsgOfEyeBlink, _brightness.value)

                println("$TAG: 👁️ Blink: $blinkCount/${faceConfigValue.value.totalEyeCount}")

                if (blinkCount >= faceConfigValue.value.totalEyeCount) {
                    currentTaskIndex++
                    _message.value = "✅ Blink detection completed!"
                    println("$TAG: ✅ All blinks completed")
                }
            }
            eyesClosedFrames = 0
        }

        wasEyesClosed = bothEyesClosed
    }

    // ===== SELFIE CAPTURE TASK =====
    private fun processSelfieTask() {
        if (_detectionState.value !is DetectionState.HoldingForCapture) {
            startHoldTimer()
        }
    }

    private fun startHoldTimer() {
        _detectionState.value = DetectionState.HoldingForCapture
        val seconds = faceConfigValue.value.captureSelfieHoldDuration / 1000
        _message.value = "⏱️ Hold still for ${seconds}s..."

        holdJob?.cancel()
        holdJob = viewModelScope.launch {
            delay(timeMillis = faceConfigValue.value.captureSelfieHoldDuration)
            captureImage()
        }
    }

    // ===== IMAGE CAPTURE =====
    private fun captureImage() {
        _message.value = "📸 Saving..."

        viewModelScope.launch {
            try {
                val bitmap = getCurrentBitmap()
                _finalSelfieBitmap.value = bitmap

                val videoPath = stopVideoRecordingAndGetPath()
                _finalSelfieVideo.value = videoPath

                delay(500)

                _detectionState.value = DetectionState.Completed(
                    imagePath = "Image captured",
                    videoPath = videoPath ?: "Video save failed"
                )

                val videoStatus = if (videoPath != null) "✓" else "✗"
                _message.value = "✅ Saved!\nImage: ✓ | Video: $videoStatus"
                _borderState.value = BorderState(color = BorderColor.DARK_GREEN)
                _processDone.value = true

                println("$TAG: ✅ Capture completed")
            } catch (e: Exception) {
                println("$TAG: ❌ Capture failed: ${e.message}")
                handleCaptureFailure("Error: ${e.message}")
            }
        }
    }

    private fun handleCaptureFailure(reason: String) {
        _detectionState.value = DetectionState.Error(reason)
        _message.value = "❌ Capture failed: $reason"
        _borderState.value = BorderState(color = BorderColor.RED)
        _processDone.value = false
    }

    // ===== ERROR HANDLERS =====
    private fun handleNoFaceDetectedWithBrightness(brightness: Float) {
        _borderState.value = BorderState(color = BorderColor.RED)

        _message.value = when {
            brightness < faceConfigValue.value.brightnessTooDarkValue.value ->
                "❌ No face detected\n🌑 Too dark"
            brightness < faceConfigValue.value.brightnessDarkValue.value ->
                "❌ No face detected\n🌓 Low light"
            brightness > faceConfigValue.value.brightnessTooBrightValue.value ->
                "❌ No face detected\n💡 Too bright"
            else ->
                "❌ Position your face in the circle"
        }

        if (isRecording) {
            stopVideoRecording()
        }

        if (_detectionState.value !is DetectionState.Completed) {
            resetDetection()
        }
        facialAttributeDetectionSession.resetAttributeDetectionSession()
    }

    // ===== VIDEO RECORDING =====
    private fun startVideoRecording() {
        if (!isRecording) {
            try {
                videoRecorder.startRecording(
                    onResult = { path -> println("$TAG: ✅ Video: $path") },
                    onError = { error ->
                        println("$TAG: ❌ Recording error: $error")
                        isRecording = false
                    }
                )
                isRecording = true
            } catch (e: Exception) {
                println("$TAG: ❌ Recording failed: ${e.message}")
                isRecording = false
            }
        }
    }

    private fun stopVideoRecording() {
        if (isRecording) {
            try {
                videoRecorder.stopRecording()
            } catch (e: Exception) {
                println("$TAG: ❌ Stop recording error: ${e.message}")
            } finally {
                isRecording = false
            }
        }
    }

    private fun stopVideoRecordingAndGetPath(): String? {
        stopVideoRecording()
        return videoRecorder.getVideoUri()
    }

    // ===== RESET METHODS =====
    private fun resetCounters() {
        moveFrameCount = 0
        eyesClosedFrames = 0
    }

    private fun resetDetection() {
        blinkCount = 0
        wasEyesClosed = false
        resetCounters()
        holdJob?.cancel()
        _detectionState.value = DetectionState.Idle

        currentTaskIndex = 0
        currentMoveIndex = 0
        currentDiagonalMoveIndex = 0
        completedMoves.clear()
        completedDiagonalMoves.clear()
        faceConfigValue.value.listOfFaceDetection.shuffled()
        faceConfigValue.value.listOfFaceDiagonalDetection.shuffled()
    }

    // ===== PUBLIC METHODS =====
    fun processDone(value: Boolean) {
        _processDone.value = value
    }

    fun resetForNewSession() {
        _processDone.value = false
        _finalSelfieBitmap.value = null
        _finalSelfieVideo.value = null
        _brightness.value = 0.5f
        _showBrightnessWarning.value = false

        resetDetection()

        _borderState.value = BorderState(color = BorderColor.RED)
        _message.value = "Position your face in the circle"

        if (isRecording) {
            stopVideoRecording()
        }

        currentBitmap = null
        frameCount = 0L
        noFaceFrameCount = 0
        baseMsgOfEyeBlink = "Start Blink your Eyes"
    }

    fun cleanup() {
        stopVideoRecording()
        currentBitmap = null
        facialAttributeDetectionSession.dispose()
    }

    override fun onCleared() {
        super.onCleared()
        stopVideoRecording()

        _processDone.value = false
        _finalSelfieBitmap.value = null
        _finalSelfieVideo.value = null
        currentBitmap = null

        blinkCount = 0
        eyesClosedFrames = 0
        wasEyesClosed = false
        frameCount = 0L
        noFaceFrameCount = 0
        moveFrameCount = 0
        currentTaskIndex = 0
        currentMoveIndex = 0
        currentDiagonalMoveIndex = 0
        completedMoves.clear()
        completedDiagonalMoves.clear()

        holdJob?.cancel()

        faceLandmarkerHelper.close()
        videoRecorder.release()
        facePoseCalculator.release()
    }
}
