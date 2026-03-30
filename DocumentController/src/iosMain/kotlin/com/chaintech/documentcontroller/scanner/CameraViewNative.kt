package com.chaintech.documentcontroller.scanner


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch
import multiplatform.network.cmpfilepicker.MediaPicker
import multiplatform.network.cmpfilepicker.MediaPickerState
import multiplatform.network.cmpfilepicker.rememberMediaPickerState
import multiplatform.network.cmpfilepicker.utils.MediaResult
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureTorchModeOff
import platform.AVFoundation.AVCaptureTorchModeOn
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.hasTorch
import platform.AVFoundation.requestAccessForMediaType
import platform.AVFoundation.setTorchMode
import platform.AVFoundation.torchMode
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

@OptIn(ExperimentalForeignApi::class)
@Composable
fun CameraViewNative(
    modifier: Modifier,
    maxSelectionCount: Int,
    onCompletion: (List<ImageBitmap?>) -> Unit,
    overlayColor: Color,
    overlayBorderColor: Color,
    overlayBorderRoundedCornerColor: Color,
    listMessages: List<String>,
    permissionDeniedView: @Composable (() -> Unit)? = null
) {
    var hasPermission by remember { mutableStateOf(false) }
    var previewView by remember { mutableStateOf<CameraPreviewView?>(null) }
    val capturedImages = remember { mutableStateListOf<ImageBitmap?>() }
    val pickerState = rememberMediaPickerState()
    var flashLightOn by remember { mutableStateOf<Boolean>(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var imageCount by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        onDispose {
            previewView?.cleanup()
        }
    }

    LaunchedEffect(Unit) {
        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> hasPermission = true
            AVAuthorizationStatusNotDetermined -> {
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                    hasPermission = granted
                }
            }

            else -> hasPermission = false
        }
    }

    GalleryPicker(maxSelectionCount = maxSelectionCount, pickerState, onPhotoPickCompletion = {
        if (it.isNotEmpty() && it.size == maxSelectionCount) {
            isProcessing = true
            onCompletion(it)
        }
    })

    Box(modifier = modifier.fillMaxSize()) {
        // Camera Preview
        CameraCaptureView(
            onPreviewViewSet = {
                previewView = it
            },
            overlayColor = overlayColor,
            overlayBorderColor = overlayBorderColor,
            overlayBorderRoundedCornerColor = overlayBorderRoundedCornerColor,
            isCameraPermissionAvailable = hasPermission,
            previousMessage = if (imageCount == 0) listMessages.first() else listMessages[imageCount - 1],
            currentMessage = listMessages[imageCount],
            flipTextAnimation = imageCount >= 1
        )

        // Bottom Controls
        BottomSheetControls(
            isCameraPermissionAvailable = hasPermission,
            isFlashLightOn = flashLightOn,
            onCaptureClick = {
                if (isProcessing || imageCount >= maxSelectionCount) return@BottomSheetControls

                isProcessing = true
                previewView?.takePhoto { byteArray ->

                    capturedImages.add(byteArray)

                    // Pass the callback if threshold reached
                    if (imageCount >= maxSelectionCount - 1) {
                        onCompletion(capturedImages)
                    }
                    if(imageCount >= maxSelectionCount - 1) {
                        return@takePhoto
                    }
                    if (imageCount < maxSelectionCount - 1) {
                        imageCount++
                    }
                    isProcessing = false
                }
            },
            onFlashLightClick = {
                val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
                if (device?.hasTorch == true) {
                    device.lockForConfiguration(null)
                    val torchOn = device.torchMode == AVCaptureTorchModeOn
                    device.setTorchMode(if (torchOn) AVCaptureTorchModeOff else AVCaptureTorchModeOn)
                    flashLightOn = !torchOn
                    device.unlockForConfiguration()
                }
            },
            onOpenGalleryPicker = {
                if (!isProcessing && imageCount < maxSelectionCount) {
                    pickerState.pickImage(maxCount = maxSelectionCount, useSkiaDecoding = false)
                }
            }
        )
    }

    if (!hasPermission) {
        if (permissionDeniedView != null) {
            permissionDeniedView()
        } else {
            PermissionDeniedView(modifier = Modifier.padding(top = 200.dp), onButtonClick = {
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                    if (!granted) {
                        openAppSettings()
                    }
                    hasPermission = granted
                }
            })
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
private fun BoxScope.CameraCaptureView(
    onPreviewViewSet: (previewView: CameraPreviewView) -> Unit,
    overlayColor: Color,
    overlayBorderColor: Color,
    overlayBorderRoundedCornerColor: Color,
    isCameraPermissionAvailable: Boolean,
    previousMessage: String,
    currentMessage: String,
    flipTextAnimation: Boolean
) {
    if (isCameraPermissionAvailable) {
        UIKitView(
            factory = {
                CameraPreviewView().apply {
                    startSession()
                    onPreviewViewSet(this)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    // Overlay
    OverlayView(
        overlayColor = overlayColor,
        overlayBorderColor = overlayBorderColor,
        overlayBorderRoundedCornerColor = overlayBorderRoundedCornerColor,
        isCameraPermissionAvailable = isCameraPermissionAvailable,
        previousMessage = previousMessage,
        currentMessage = currentMessage,
        flipTextAnimation = flipTextAnimation
    )
}

@Composable
private fun PermissionDeniedView(modifier: Modifier, onButtonClick: () -> Unit) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Please grant permission to access your camera. You can manage permissions in your device settings.",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = Color.White
        )

        Button(
            onClick = onButtonClick,
            colors = ButtonDefaults.buttonColors(
                contentColor = Color.White,
                containerColor = Color.White
            ),
            shape = RoundedCornerShape(15.dp),
            modifier = Modifier.padding(top = 25.dp)
        ) {
            Text("Allow", color = Color.Black, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun BoxScope.BottomSheetControls(
    isCameraPermissionAvailable: Boolean,
    isFlashLightOn: Boolean,
    onCaptureClick: () -> Unit,
    onFlashLightClick: () -> Unit,
    isCaptureEnabled: Boolean = true,
    onOpenGalleryPicker: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .background(
                color = Color(0xFF1C1C1E), // Dark background
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .padding(vertical = 22.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Center Properly",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(9.dp))
        Text(
            text = "Place ID on a plain surface\nConfirm entire ID is visible and clear",
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Icon(
                imageVector = galleryImage(),
                contentDescription = "Gallery",
                tint = Color.White,
                modifier = Modifier
                    .size(32.dp)
                    .clickable {
                        onOpenGalleryPicker?.invoke()
                    }
            )

            if (isCameraPermissionAvailable) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            if (isCaptureEnabled) Color(0xFF00C2CB) else Color.Gray,
                            shape = CircleShape
                        )
                        .clickable(enabled = isCaptureEnabled) { onCaptureClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("OK", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp),
                    contentAlignment = Alignment.Center
                ) {}
            }


            Icon(
                imageVector = if (isFlashLightOn) flashOn() else flashOff(),
                contentDescription = "Flashlight",
                tint = Color.White,
                modifier = Modifier
                    .size(32.dp)
                    .clickable {
                        onFlashLightClick()
                    }
            )
        }
    }
}

private fun flashOn(): ImageVector {
    return materialIcon(name = "Filled.FlashOn") {
        materialPath {
            moveTo(7.0f, 2.0f)
            verticalLineToRelative(11.0f)
            horizontalLineToRelative(3.0f)
            verticalLineToRelative(9.0f)
            lineToRelative(7.0f, -12.0f)
            horizontalLineToRelative(-4.0f)
            lineToRelative(4.0f, -8.0f)
            close()
        }
    }
}

private fun flashOff(): ImageVector {
    return materialIcon(name = "Filled.FlashOff") {
        materialPath {
            moveTo(3.27f, 3.0f)
            lineTo(2.0f, 4.27f)
            lineToRelative(5.0f, 5.0f)
            verticalLineTo(13.0f)
            horizontalLineToRelative(3.0f)
            verticalLineToRelative(9.0f)
            lineToRelative(3.58f, -6.14f)
            lineTo(17.73f, 20.0f)
            lineTo(19.0f, 18.73f)
            lineTo(3.27f, 3.0f)
            close()
            moveTo(17.0f, 10.0f)
            horizontalLineToRelative(-4.0f)
            lineToRelative(4.0f, -8.0f)
            horizontalLineTo(7.0f)
            verticalLineToRelative(2.18f)
            lineToRelative(8.46f, 8.46f)
            lineTo(17.0f, 10.0f)
            close()
        }
    }
}

private fun galleryImage(): ImageVector {
    return materialIcon(name = "Filled.Image") {
        materialPath {
            moveTo(21.0f, 19.0f)
            verticalLineTo(5.0f)
            curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
            horizontalLineTo(5.0f)
            curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
            verticalLineToRelative(14.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(14.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            close()
            moveTo(8.5f, 13.5f)
            lineToRelative(2.5f, 3.01f)
            lineTo(14.5f, 12.0f)
            lineToRelative(4.5f, 6.0f)
            horizontalLineTo(5.0f)
            lineToRelative(3.5f, -4.5f)
            close()
        }
    }
}

@Composable
private fun GalleryPicker(
    maxSelectionCount: Int,
    openImagePicker: MediaPickerState,
    onPhotoPickCompletion: (List<ImageBitmap?>) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val imageArrayList = ArrayList<ImageBitmap?>()
    MediaPicker(
        state = openImagePicker,
        onResult = { result ->
            when (result) {
                is MediaResult.Image -> {
                    coroutineScope.launch {
                        imageArrayList.addAll(result.bitmap)
                        if (imageArrayList.size == maxSelectionCount) {
                            onPhotoPickCompletion(result.bitmap)
                        }
                    }
                }
                else -> {}
            }
        },
        onPermissionDenied = { deniedPermission ->
            // Show a permission denied dialog
        }
    )
}

fun openAppSettings() {
    val settingsUrl = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
    if (settingsUrl != null && UIApplication.sharedApplication.canOpenURL(settingsUrl)) {
        UIApplication.sharedApplication.openURL(settingsUrl)
    }
}