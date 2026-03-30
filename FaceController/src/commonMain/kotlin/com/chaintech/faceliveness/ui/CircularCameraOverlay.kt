package com.chaintech.faceliveness.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.chaintech.faceliveness.base.FaceMoveDetection
import com.chaintech.faceliveness.base.FaceMoveDiagonalDetection
import com.chaintech.faceliveness.util.BorderColor
import com.chaintech.faceliveness.util.BorderState
import kotlinx.coroutines.delay

@Composable
fun CircularCameraOverlay(
    borderState: BorderState,
    modifier: Modifier = Modifier
) {
    val isBlinking = remember { mutableStateOf(false) }

    LaunchedEffect(borderState) {
        while (true) {
            isBlinking.value = true
            delay(400)
            isBlinking.value = false
            delay(400)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val centerX = canvasWidth / 2
        val centerY = canvasHeight / 2
        val radius = minOf(canvasWidth, canvasHeight) / 2f - 6f

        val strokeWidth = 12f

        val lightGreen = Color(0xFF60AB63)
        val darkGreen = Color(0xFF05E514)
        val lightBlue = Color(0xFF00BCD4)
        val red = Color(0xFFE53935)

        val baseColor = when (borderState.color) {
            BorderColor.RED -> red
            BorderColor.LIGHT_GREEN -> lightGreen
            BorderColor.DARK_GREEN -> darkGreen
        }

        // ✅ If no moves, draw full circle
        if (borderState.totalMoves.isEmpty() && borderState.totalDiagonalMoves.isEmpty()) {
            drawCircle(
                color = baseColor,
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            return@Canvas
        }

        // ✅ Calculate total moves and sweep angle
        val totalMoveCount = borderState.totalMoves.size + borderState.totalDiagonalMoves.size
        val sweepAnglePerMove = 360f / totalMoveCount

        // ✅ Get current move
        val currentMove = borderState.getCurrentMove()

        println("🎨 ════════════════════════════════════════")
        println("🎨 Total: $totalMoveCount arcs, sweep: $sweepAnglePerMove°")
        println("🎨 Regular: ${borderState.totalMoves}")
        println("🎨 Diagonal: ${borderState.totalDiagonalMoves}")
        println("🎨 👉 CURRENT: $currentMove")
        println("🎨 ════════════════════════════════════════")

        // ✅ Draw REGULAR moves
        for ((index, move) in borderState.totalMoves.withIndex()) {
            val arcData = getArcDataForMove(move, sweepAnglePerMove)
            val isCompleted = borderState.completedMoves.contains(move)
            val isCurrent = currentMove == move

            val arcColor = when {
                isCompleted -> darkGreen
                isCurrent && isBlinking.value -> lightBlue
                isCurrent -> lightGreen
                else -> lightGreen
            }

            val status = when {
                isCompleted -> "✓ COMPLETED"
                isCurrent && isBlinking.value -> "💫 BLINKING"
                isCurrent -> "👉 CURRENT"
                else -> "⏳ PENDING"
            }

            println("🎨 [R$index] $move")
            println("   ${arcData.label} | Start: ${arcData.startAngle}° | Sweep: $sweepAnglePerMove°")
            println("   Status: $status")

            drawArc(
                color = arcColor,
                startAngle = arcData.startAngle,
                sweepAngle = sweepAnglePerMove,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // ✅ Draw DIAGONAL moves
        for ((index, move) in borderState.totalDiagonalMoves.withIndex()) {
            val arcData = getArcDataForDiagonalMove(move, sweepAnglePerMove)
            val isCompleted = borderState.completedDiagonalMoves.contains(move)
            val isCurrent = currentMove == move

            val arcColor = when {
                isCompleted -> darkGreen
                isCurrent && isBlinking.value -> lightBlue
                isCurrent -> lightGreen
                else -> lightGreen
            }

            val status = when {
                isCompleted -> "✓ COMPLETED"
                isCurrent && isBlinking.value -> "💫 BLINKING"
                isCurrent -> "👉 CURRENT"
                else -> "⏳ PENDING"
            }

            println("🎨 [D$index] $move")
            println("   ${arcData.label} | Start: ${arcData.startAngle}° | Sweep: $sweepAnglePerMove°")
            println("   Status: $status")

            drawArc(
                color = arcColor,
                startAngle = arcData.startAngle,
                sweepAngle = sweepAnglePerMove,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

data class ArcData(
    val startAngle: Float,
    val label: String
)

// ✅ FIXED: Correct angles for regular moves
private fun getArcDataForMove(move: FaceMoveDetection, sweepAngle: Float): ArcData {
    return when (move) {
        FaceMoveDetection.MOVE_TOP -> {
            // TOP = 270° (12 o'clock position)
            ArcData(
                startAngle = normalizeAngle(270f - sweepAngle / 2f),
                label = "TOP (270°)"
            )
        }
        FaceMoveDetection.MOVE_RIGHT -> {
            // RIGHT = 0° (3 o'clock position)
            val start = -sweepAngle / 2f
            ArcData(
                startAngle = normalizeAngle(start),
                label = "RIGHT (0°)"
            )
        }
        FaceMoveDetection.MOVE_BOTTOM -> {
            // BOTTOM = 90° (6 o'clock position)
            ArcData(
                startAngle = normalizeAngle(90f - sweepAngle / 2f),
                label = "BOTTOM (90°)"
            )
        }
        FaceMoveDetection.MOVE_LEFT -> {
            // LEFT = 180° (9 o'clock position)
            ArcData(
                startAngle = normalizeAngle(180f - sweepAngle / 2f),
                label = "LEFT (180°)"
            )
        }
    }
}

// ✅ FIXED: Correct angles for diagonal moves
private fun getArcDataForDiagonalMove(move: FaceMoveDiagonalDetection, sweepAngle: Float): ArcData {
    return when (move) {
        FaceMoveDiagonalDetection.MOVE_TOP_LEFT -> {
            // TOP-LEFT = 225° (10:30 position)
            ArcData(
                startAngle = normalizeAngle(225f - sweepAngle / 2f),
                label = "TOP-LEFT (225°)"
            )
        }
        FaceMoveDiagonalDetection.MOVE_TOP_RIGHT -> {
            // TOP-RIGHT = 315° (1:30 position)
            ArcData(
                startAngle = normalizeAngle(315f - sweepAngle / 2f),
                label = "TOP-RIGHT (315°)"
            )
        }
        FaceMoveDiagonalDetection.MOVE_BOTTOM_LEFT -> {
            // BOTTOM-LEFT = 135° (7:30 position)
            ArcData(
                startAngle = normalizeAngle(135f - sweepAngle / 2f),
                label = "BOTTOM-LEFT (135°)"
            )
        }
        FaceMoveDiagonalDetection.MOVE_BOTTOM_RIGHT -> {
            // BOTTOM-RIGHT = 45° (4:30 position)
            ArcData(
                startAngle = normalizeAngle(45f - sweepAngle / 2f),
                label = "BOTTOM-RIGHT (45°)"
            )
        }
    }
}

private fun normalizeAngle(angle: Float): Float {
    var normalized = angle % 360f
    if (normalized < 0) normalized += 360f
    return normalized
}
