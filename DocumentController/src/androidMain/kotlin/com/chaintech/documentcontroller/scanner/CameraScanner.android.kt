package com.chaintech.documentcontroller.scanner

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

@Composable
actual fun DocScannerView(
    maxSelectionCount: Int,
    modifier: Modifier,
    onCompletion: (List<ImageBitmap?>?) -> Unit,
    overlayColor: Color,
    overlayBorderColor: Color,
    overlayBorderRoundedCornerColor: Color,
    listMessages: List<String>,
    permissionDeniedView: @Composable (() -> Unit)?,
) {
    CameraViewNative(
        modifier = modifier,
        onCompletion = onCompletion,
        overlayColor = overlayColor,
        overlayBorderColor = overlayBorderColor,
        permissionDeniedView = permissionDeniedView,
        overlayBorderRoundedCornerColor = overlayBorderRoundedCornerColor,
        listMessages = listMessages,
        maxSelectionCount = maxSelectionCount
    )
}