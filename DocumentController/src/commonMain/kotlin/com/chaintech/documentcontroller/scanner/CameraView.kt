package com.chaintech.documentcontroller.scanner

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

@Composable
fun DocumentScanner(
    modifier: Modifier,
    onCompletion: (List<ImageBitmap?>?) -> Unit,
    overlayColor: Color = Color(0x88000000),
    overlayBorderColor: Color = Color(0xFF4A4A49),
    overlayBorderRoundedCornerColor: Color = Color.White,
    noOfDocuments: Int = 1,
    listMessages: List<String> = if (noOfDocuments == 1) arrayListOf("Scan Your Document Image") else arrayListOf(
        "Scan front page of your ID Card",
        "Scan back page of your ID Card"
    ),
    permissionDeniedView: @Composable (() -> Unit)? = null,
) {
    CameraView(
        maxSelectionCount = noOfDocuments,
        modifier = modifier,
        onCompletion = onCompletion,
        overlayColor = overlayColor,
        overlayBorderColor = overlayBorderColor,
        overlayBorderRoundedCornerColor = overlayBorderRoundedCornerColor,
        listMessages = listMessages,
        permissionDeniedView = permissionDeniedView,
    )
}

@Composable
private fun CameraView(
    maxSelectionCount: Int,
    modifier: Modifier,
    onCompletion: (List<ImageBitmap?>?) -> Unit,
    overlayColor: Color,
    overlayBorderColor: Color,
    overlayBorderRoundedCornerColor: Color,
    listMessages: List<String>,
    permissionDeniedView: @Composable (() -> Unit)?,
) {
    DocScannerView(
        modifier = modifier,
        onCompletion = onCompletion,
        overlayColor = overlayColor,
        overlayBorderColor = overlayBorderColor,
        overlayBorderRoundedCornerColor = overlayBorderRoundedCornerColor,
        permissionDeniedView = permissionDeniedView,
        listMessages = listMessages,
        maxSelectionCount = maxSelectionCount
    )
}
