package com.chaintech.faceliveness.util

import com.chaintech.faceliveness.base.FaceMoveDetection
import com.chaintech.faceliveness.base.FaceMoveDiagonalDetection

data class BorderState(
    val color: BorderColor = BorderColor.LIGHT_GREEN,
    val completedMoves: List<FaceMoveDetection> = emptyList(),
    val completedDiagonalMoves: List<FaceMoveDiagonalDetection> = emptyList(),
    val totalMoves: List<FaceMoveDetection> = emptyList(),
    val totalDiagonalMoves: List<FaceMoveDiagonalDetection> = emptyList()
) {
    // ✅ Helper functions
    fun isMoveCompleted(move: FaceMoveDetection): Boolean = completedMoves.contains(move)
    fun isDiagonalMoveCompleted(move: FaceMoveDiagonalDetection): Boolean = completedDiagonalMoves.contains(move)

    // ✅ Get total count of all moves (regular + diagonal)
    fun getTotalMoveCount(): Int = totalMoves.size + totalDiagonalMoves.size

    // ✅ Get current move (respects order: regular moves first, then diagonal)
    fun getCurrentMove(): Any? {
        // First check regular moves
        val currentRegular = totalMoves.firstOrNull { !completedMoves.contains(it) }
        if (currentRegular != null) return currentRegular

        // Then check diagonal moves
        return totalDiagonalMoves.firstOrNull { !completedDiagonalMoves.contains(it) }
    }
}

enum class BorderColor {
    RED,
    LIGHT_GREEN,
    DARK_GREEN
}

// Keep existing classes
data class FacePosition(
    val isFaceDetected: Boolean = false,
    val isInCenter: Boolean = false,
    val faceInCircle: Boolean = false,
    val isMovedLeft: Boolean = false,
    val isMovedRight: Boolean = false,
    val isMovedTop: Boolean = false,
    val isMovedBottom: Boolean = false,
    val isMovedTopLeft: Boolean = false,
    val isMovedTopRight: Boolean = false,
    val isMovedBottomLeft: Boolean = false,
    val isMovedBottomRight: Boolean = false,
    val isFacingStraight: Boolean = false,
    val yawAngle: Double = 0.0,
    val pitchAngle: Double = 0.0,
    val isLeftEyeClosed: Boolean = false,
    val isRightEyeClosed: Boolean = false,

    // ✅ NEW: Face distance status
    val isFaceTooFar: Boolean = false,
    val isFaceTooClose: Boolean = false,
    val faceSize: Float = 0f,  // Face width as percentage of frame

)

sealed class DetectionState {
    data object Idle : DetectionState()
    data object BlinkDetection : DetectionState()
    data object HoldingForCapture : DetectionState()
    data class Completed(val imagePath: String, val videoPath: String) : DetectionState()
    data class Error(val message: String) : DetectionState()
}
