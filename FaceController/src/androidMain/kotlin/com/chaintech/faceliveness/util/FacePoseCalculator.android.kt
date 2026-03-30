@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.chaintech.faceliveness.util

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlin.math.*

actual class FacePoseCalculator {
    companion object {
        private const val TAG = "FacePoseCalculator"

        // ------- Landmark indices -------
        private const val NOSE_TIP = 4
        private const val LEFT_EYE_LEFT_CORNER = 263
        private const val RIGHT_EYE_RIGHT_CORNER = 33
        private const val FOREHEAD = 10
        private const val CHIN = 152
        private const val LEFT_CHEEK = 234
        private const val RIGHT_CHEEK = 454
        private const val LEFT_TEMPLE = 127
        private const val RIGHT_TEMPLE = 356
        private const val LEFT_EYE_TOP = 159
        private const val LEFT_EYE_BOTTOM = 145
        private const val RIGHT_EYE_TOP = 386
        private const val RIGHT_EYE_BOTTOM = 374

        // ------- Yaw/Pitch thresholds for MOVEMENT detection -------
        private const val YAW_THRESHOLD_LEFT = 0.15f
        private const val YAW_THRESHOLD_RIGHT = -0.15f
        private const val YAW_CENTER_TOLERANCE = 0.12f

        private const val PITCH_THRESHOLD_UP = -0.15f
        private const val PITCH_THRESHOLD_DOWN = 0.15f
        private const val PITCH_CENTER_TOLERANCE = 0.12f

        private const val DIAGONAL_YAW_THRESHOLD = 0.15f
        private const val DIAGONAL_PITCH_THRESHOLD = 0.15f

        private const val EYE_CLOSURE_THRESHOLD = 0.017f
        private const val CIRCLE_RADIUS = 0.40f

        // ------- Distance thresholds -------
        private const val MIN_FACE_SIZE = 0.27f  // too far if smaller
        private const val MAX_FACE_SIZE = 0.42f  // too close if bigger

        // ------- Rotation threshold for skipping distance check -------
        private const val YAW_ROTATION_SKIP_THRESHOLD = 0.12f

        // Baseline pitch when looking straight (nose is naturally below eyes)
        private const val BASELINE_PITCH = 0.35f
        private const val PITCH_ROTATION_SKIP_THRESHOLD = 0.20f

        // Grace period after rotation to avoid false distance errors
        private const val POST_ROTATION_GRACE_FRAMES = 15
    }

    private var frameCount = 0
    private val distanceSmoother = ValueSmoother(alpha = 0.15f) // Slower smoothing
    private var lastValidDistanceOk = true

    // For calibrating baseline pitch
    private var baselinePitch: Float? = null
    private var calibrationFrames = 0
    private val calibrationFrameCount = 10

    // Track rotation state for grace period
    private var wasHeadRotated = false
    private var framesSinceRotation = 0

    actual fun calculatePose(result: Any): FacePosition {
        val faceLandmarkerResult = result as FaceLandmarkerResult
        if (faceLandmarkerResult.faceLandmarks().isEmpty()) {
            resetFilters()
            return FacePosition()
        }

        val landmarks = faceLandmarkerResult.faceLandmarks()[0]

        val noseTip = landmarks[NOSE_TIP]
        val leftEye = landmarks[LEFT_EYE_LEFT_CORNER]
        val rightEye = landmarks[RIGHT_EYE_RIGHT_CORNER]
        val forehead = landmarks[FOREHEAD]
        val chin = landmarks[CHIN]
        val leftCheek = landmarks[LEFT_CHEEK]
        val rightCheek = landmarks[RIGHT_CHEEK]

        // ====== CALCULATE MEASUREMENTS ======
        val faceWidth = abs(rightCheek.x() - leftCheek.x())
        val faceHeight = abs(forehead.y() - chin.y())

        val eyeDistance = sqrt(
            (rightEye.x() - leftEye.x()).pow(2) +
                    (rightEye.y() - leftEye.y()).pow(2)
        )

        val eyeCenterX = (leftEye.x() + rightEye.x()) / 2.0f
        val eyeCenterY = (leftEye.y() + rightEye.y()) / 2.0f

        val noseOffsetX = noseTip.x() - eyeCenterX
        val noseOffsetY = noseTip.y() - eyeCenterY

        val rawYaw = if (eyeDistance > 0.01f) noseOffsetX / eyeDistance else 0f
        val rawPitch = if (eyeDistance > 0.01f) noseOffsetY / eyeDistance else 0f

        // ====== CALIBRATE BASELINE PITCH ======
        if (calibrationFrames < calibrationFrameCount) {
            baselinePitch = baselinePitch?.let { (it + rawPitch) / 2f } ?: rawPitch
            calibrationFrames++
        }

        val effectiveBaselinePitch = baselinePitch ?: BASELINE_PITCH
        val normalizedPitch = rawPitch - effectiveBaselinePitch

        // ====== CHECK IF HEAD IS SIGNIFICANTLY ROTATED ======
        val isSignificantYawRotation = abs(rawYaw) > YAW_ROTATION_SKIP_THRESHOLD
        val isSignificantPitchRotation = abs(normalizedPitch) > PITCH_ROTATION_SKIP_THRESHOLD
        val isHeadRotated = isSignificantYawRotation || isSignificantPitchRotation

        // ====== TRACK ROTATION STATE WITH GRACE PERIOD ======
        if (isHeadRotated) {
            wasHeadRotated = true
            framesSinceRotation = 0
        } else if (wasHeadRotated) {
            framesSinceRotation++
            if (framesSinceRotation > POST_ROTATION_GRACE_FRAMES) {
                wasHeadRotated = false
            }
        }

        // Skip distance check if currently rotated OR recently was rotated
        val shouldSkipDistanceCheck = isHeadRotated || wasHeadRotated

        // ====== DISTANCE COMPUTATION ======
        val combinedSize = (faceHeight * 0.5f) + (faceWidth * 0.25f) + (eyeDistance * 0.25f)
        val smoothSize = if (shouldSkipDistanceCheck) {
            distanceSmoother.currentValue ?: distanceSmoother.smooth(combinedSize)
        } else {
            distanceSmoother.smooth(combinedSize)
        }

        val isFaceTooFar: Boolean
        val isFaceTooClose: Boolean

        if (shouldSkipDistanceCheck) {
            isFaceTooFar = false
            isFaceTooClose = false
        } else {
            isFaceTooFar = smoothSize < MIN_FACE_SIZE
            isFaceTooClose = smoothSize > MAX_FACE_SIZE
            lastValidDistanceOk = !isFaceTooFar && !isFaceTooClose
        }

        frameCount++
        printLogs("$TAG: smoothSize=${"%.3f".format(smoothSize)}, isHeadRotated=$isHeadRotated, skipDistance=$shouldSkipDistanceCheck, graceFrames=$framesSinceRotation")

        val isFullyInCircle = checkFaceInCircularBoundary(landmarks)

        // Return early for distance issues (only when not skipping)
        if (isFaceTooFar) {
            printLogs("$TAG: ⚠️ Face too far (smoothSize=${"%.3f".format(smoothSize)})")
            return FacePosition(
                isFaceDetected = true,
                faceInCircle = isFullyInCircle,
                isFaceTooFar = true,
                isFaceTooClose = false,
                faceSize = smoothSize
            )
        }

        if (isFaceTooClose) {
            printLogs("$TAG: ⚠️ Face too close (smoothSize=${"%.3f".format(smoothSize)})")
            return FacePosition(
                isFaceDetected = true,
                faceInCircle = isFullyInCircle,
                isFaceTooFar = false,
                isFaceTooClose = true,
                faceSize = smoothSize
            )
        }

        // if not in circle but distance is ok
        if (!isFullyInCircle) {
            return FacePosition(
                isFaceDetected = true,
                faceInCircle = false,
                faceSize = smoothSize
            )
        }

        // ====== MOVEMENT DETECTION ======
        val noseChinDistance = chin.y() - noseTip.y()
        val noseRelativePosition = if (faceHeight > 0f) noseChinDistance / faceHeight else 0f

        val isMovedRight = rawYaw > YAW_THRESHOLD_LEFT
        val isMovedLeft = rawYaw < YAW_THRESHOLD_RIGHT

        val isMovedTop1 = normalizedPitch < PITCH_THRESHOLD_UP
        val isMovedBottom1 = normalizedPitch > PITCH_THRESHOLD_DOWN
        val isMovedTop2 = noseRelativePosition > 0.65f
        val isMovedBottom2 = noseRelativePosition < 0.35f
        val isMovedTop = isMovedTop1 || isMovedTop2
        val isMovedBottom = isMovedBottom1 || isMovedBottom2

        val isMovedTopLeft =
            (rawYaw < -DIAGONAL_YAW_THRESHOLD) && (normalizedPitch < -DIAGONAL_PITCH_THRESHOLD || isMovedTop)
        val isMovedTopRight =
            (rawYaw > DIAGONAL_YAW_THRESHOLD) && (normalizedPitch < -DIAGONAL_PITCH_THRESHOLD || isMovedTop)
        val isMovedBottomLeft =
            (rawYaw < -DIAGONAL_YAW_THRESHOLD) && (normalizedPitch > DIAGONAL_PITCH_THRESHOLD || isMovedBottom)
        val isMovedBottomRight =
            (rawYaw > DIAGONAL_YAW_THRESHOLD) && (normalizedPitch > DIAGONAL_PITCH_THRESHOLD || isMovedBottom)

        val isInCenterRange =
            abs(rawYaw) < YAW_CENTER_TOLERANCE && abs(normalizedPitch) < PITCH_CENTER_TOLERANCE

        // Blink detection
        val leftEyeTop = landmarks[LEFT_EYE_TOP]
        val leftEyeBottom = landmarks[LEFT_EYE_BOTTOM]
        val rightEyeTop = landmarks[RIGHT_EYE_TOP]
        val rightEyeBottom = landmarks[RIGHT_EYE_BOTTOM]

        val leftEyeHeight = abs(leftEyeTop.y() - leftEyeBottom.y())
        val rightEyeHeight = abs(rightEyeTop.y() - rightEyeBottom.y())

        val isLeftEyeClosed = leftEyeHeight < EYE_CLOSURE_THRESHOLD
        val isRightEyeClosed = rightEyeHeight < EYE_CLOSURE_THRESHOLD

        printLogs("$TAG: yaw=${"%.3f".format(rawYaw)}, normalizedPitch=${"%.3f".format(normalizedPitch)}")

        return FacePosition(
            isFaceDetected = true,
            isInCenter = isInCenterRange,
            faceInCircle = true,
            isFaceTooFar = false,
            isFaceTooClose = false,
            faceSize = smoothSize,
            isMovedLeft = isMovedLeft,
            isMovedRight = isMovedRight,
            isMovedTop = isMovedTop,
            isMovedBottom = isMovedBottom,
            isMovedTopLeft = isMovedTopLeft,
            isMovedTopRight = isMovedTopRight,
            isMovedBottomLeft = isMovedBottomLeft,
            isMovedBottomRight = isMovedBottomRight,
            isFacingStraight = isInCenterRange,
            yawAngle = (rawYaw * 100.0f).toDouble(),
            pitchAngle = (normalizedPitch * 100.0f).toDouble(),
            isLeftEyeClosed = isLeftEyeClosed,
            isRightEyeClosed = isRightEyeClosed
        )
    }

    private fun checkFaceInCircularBoundary(landmarks: List<NormalizedLandmark>): Boolean {
        val forehead = landmarks[FOREHEAD]
        val chin = landmarks[CHIN]
        val leftCheek = landmarks[LEFT_CHEEK]
        val rightCheek = landmarks[RIGHT_CHEEK]
        val leftTemple = landmarks[LEFT_TEMPLE]
        val rightTemple = landmarks[RIGHT_TEMPLE]

        val centerX = 0.5f
        val centerY = 0.5f

        val boundaryPoints = listOf(forehead, chin, leftCheek, rightCheek, leftTemple, rightTemple)

        for (point in boundaryPoints) {
            val dx = point.x() - centerX
            val dy = point.y() - centerY
            val distance = sqrt(dx * dx + dy * dy)

            if (distance > CIRCLE_RADIUS) {
                return false
            }
        }

        val faceWidth = abs(rightCheek.x() - leftCheek.x())
        val faceHeight = abs(forehead.y() - chin.y())

        return faceWidth >= 0.10f && faceHeight >= 0.10f &&
                faceWidth <= 0.75f && faceHeight <= 0.75f
    }

    actual fun release() {
        resetFilters()
    }

    private fun resetFilters() {
        distanceSmoother.reset()
        lastValidDistanceOk = true
        baselinePitch = null
        calibrationFrames = 0
        wasHeadRotated = false
        framesSinceRotation = 0
    }

    private class ValueSmoother(private val alpha: Float = 0.15f) {
        private var value: Float? = null

        val currentValue: Float?
            get() = value

        fun smooth(v: Float): Float {
            value = value?.let { it + alpha * (v - it) } ?: v
            return value!!
        }

        fun reset() {
            value = null
        }
    }

    private fun printLogs(message: String) {
        if (frameCount % 30 == 0) {
            println(message)
        }
    }
}
