package com.chaintech.documentcontroller.scanner

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OverlayView(
    overlayColor: Color = Color(0x88000000),
    overlayBorderColor: Color = Color(0xFF4A4A49),
    overlayBorderRoundedCornerColor: Color = Color.White,
    isCameraPermissionAvailable: Boolean,
    previousMessage: String = "Align your ID within the box",
    currentMessage: String = "Align your ID within the box",
    flipTextAnimation: Boolean = false
) {
    if (!isCameraPermissionAvailable) {
        Box(
            modifier = Modifier
                .background(Color.Black)
                .fillMaxSize()
        )
        return
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val density = LocalDensity.current
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()

        // Compute scan area size & position dynamically
        val scanAreaWidth = screenWidth * 0.85f
        val scanAreaHeight = screenHeight * 0.25f
        val left = (screenWidth - scanAreaWidth) / 2
        val top = (screenHeight - scanAreaHeight) / 4
        val bottomOfScanBox = top + scanAreaHeight

        // Draw overlay and border
        Canvas(modifier = Modifier.matchParentSize()) {
            val scanAreaRect = Rect(
                offset = Offset(left, top),
                size = Size(scanAreaWidth, scanAreaHeight)
            )

            val cornerRadius = 16.dp.toPx()
            val cutoutPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = scanAreaRect,
                        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                    )
                )
            }

            // Dark overlay outside the scan area
            clipPath(cutoutPath, clipOp = ClipOp.Difference) {
                drawRect(color = overlayColor, size = size)
            }

            // Border of scan box
            val borderStroke = 1.dp.toPx()
            drawRoundRect(
                color = overlayBorderColor,
                topLeft = scanAreaRect.topLeft,
                size = scanAreaRect.size,
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(width = borderStroke)
            )

            // Highlight corners
            val highlightRadius = 15.dp.toPx()
            val highlightStroke = 3.dp.toPx()

            fun drawCornerArc(
                startAngle: Float,
                sweepAngle: Float,
                offsetX: Float,
                offsetY: Float
            ) {
                drawArc(
                    color = overlayBorderRoundedCornerColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(offsetX, offsetY),
                    size = Size(highlightRadius * 2, highlightRadius * 2),
                    style = Stroke(width = highlightStroke)
                )
            }

            drawCornerArc(180f, 90f, left, top)
            drawCornerArc(270f, 90f, left + scanAreaWidth - highlightRadius * 2, top)
            drawCornerArc(90f, 90f, left, top + scanAreaHeight - highlightRadius * 2)
            drawCornerArc(
                0f,
                90f,
                left + scanAreaWidth - highlightRadius * 2,
                top + scanAreaHeight - highlightRadius * 2
            )
        }

        // Text Animation Part
        var showBack by remember { mutableStateOf(false) }
        val rotationY = remember { Animatable(0f) }

        LaunchedEffect(flipTextAnimation) {
            if (flipTextAnimation) {
                rotationY.animateTo(
                    targetValue = 180f,
                    animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
                )
            }
        }

        // Change text midway through flip (at 90°)
        LaunchedEffect(rotationY.value) {
            if (rotationY.value >= 90f && !showBack) {
                showBack = true
            }
        }

        val displayText = if (showBack) currentMessage else previousMessage
        val adjustedRotationY = if (showBack) rotationY.value - 180f else rotationY.value

        // Text message positioned right below the scan area
        Text(
            text = displayText,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(
                    start = 10.dp,
                    end = 10.dp,
                    top = with(LocalDensity.current) { bottomOfScanBox.toDp() + 35.dp })
                .fillMaxWidth()
                .graphicsLayer(
                    rotationY = adjustedRotationY,
                    transformOrigin = TransformOrigin(0.5f, 0.5f),
                    cameraDistance = 12 * density.density
                )
        )
    }
}