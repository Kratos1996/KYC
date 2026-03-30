package com.chaintech.faceliveness.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import com.chaintech.faceliveness.base.FaceConfig

// Expect composable for platform-specific camera implementation
@Composable
expect fun CameraView(
    faceConfig: FaceConfig,
    onProcessComplete: (image: ImageBitmap, imagePath: String, videoPath: String) -> Unit,
    onClose:()->Unit,
    modifier: Modifier = Modifier
)