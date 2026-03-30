package com.chaintech.documentcontroller.scanner

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

@Composable
expect fun DocScannerView(
    maxSelectionCount: Int, modifier: Modifier,
    onCompletion: (List<ImageBitmap?>?) -> Unit,
    overlayColor: Color, overlayBorderColor: Color,
    overlayBorderRoundedCornerColor: Color,
    listMessages: List<String>,
    permissionDeniedView: @Composable (() -> Unit)? = null,
)