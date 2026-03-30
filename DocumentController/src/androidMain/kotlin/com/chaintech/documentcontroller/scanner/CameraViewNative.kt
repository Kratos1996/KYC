package com.chaintech.documentcontroller.scanner

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.chaintech.documentcontroller.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.common.util.concurrent.ListenableFuture
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import kotlinx.coroutines.launch
import multiplatform.network.cmpfilepicker.MediaPicker
import multiplatform.network.cmpfilepicker.MediaPickerState
import multiplatform.network.cmpfilepicker.rememberMediaPickerState
import multiplatform.network.cmpfilepicker.utils.MediaResult

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraViewNative(
    maxSelectionCount: Int,
    modifier: Modifier,
    onCompletion: (List<ImageBitmap?>) -> Unit,
    overlayColor: Color,
    overlayBorderColor: Color,
    overlayBorderRoundedCornerColor: Color,
    listMessages: List<String>,
    permissionDeniedView: @Composable (() -> Unit)? = null
) {
    if (maxSelectionCount != listMessages.size) {
        return
    }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }
    }
    var isProcessing by remember { mutableStateOf(false) }
    var imageCount by remember { mutableIntStateOf(0) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var preview by remember { mutableStateOf<Preview?>(null) }
    var flashLightOn by remember { mutableStateOf<Boolean>(false) }
    var hasCamPermission by remember { mutableStateOf(value = false) }
    var permissionRationalDialog by remember { mutableStateOf(value = false) }
    var launchSetting by remember { mutableStateOf(value = false) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setFlashMode(ImageCapture.FLASH_MODE_OFF)
            .build()
    }
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val capturedImages = remember { mutableStateListOf<ImageBitmap?>() }
    val pickerState = rememberMediaPickerState()

    if (cameraPermissionState.status.isGranted) {
        hasCamPermission = true
    } else {
        hasCamPermission = false
        permissionRationalDialog = true
    }
    if (launchSetting) {
        OpenSettings()
        launchSetting = false
    }

    val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
        ProcessCameraProvider.getInstance(context)
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraProviderFuture.get().unbindAll()
            cameraExecutor.shutdown()
        }
    }

    LaunchedEffect(Unit) {
        cameraPermissionState.launchPermissionRequest()
    }

    LaunchedEffect(cameraPermissionState.status) {
        if (cameraPermissionState.status.isGranted) {
            hasCamPermission = true
        }
    }

    LaunchedEffect(flashLightOn) {
        if (hasCamPermission) {
            camera?.cameraControl?.enableTorch(flashLightOn)
        }
    }

    LaunchedEffect(hasCamPermission) {
        // This effect is triggered when flashLightOn state changes
        if (hasCamPermission) {
            // Rebind the camera with the updated flash state
            cameraProviderFuture.get().unbindAll()

            val cameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            preview = Preview.Builder().build().apply {
                surfaceProvider = previewView.surfaceProvider
            }

            try {
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )

                if (camera?.cameraInfo?.hasFlashUnit() == true) {
                    camera?.cameraControl?.enableTorch(flashLightOn)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    //Gallery picker view
    GalleryPicker(
        maxSelectionCount = maxSelectionCount,
        openImagePicker = pickerState,
        onPhotoPickCompletion = {
            if (it.isNotEmpty() && it.size == maxSelectionCount) {
                isProcessing = true
                onCompletion(it)
            }
        })

    //Camera & Preview & Bottom view
    Box(modifier = modifier.fillMaxSize()) {
        //Camera View
        CameraCaptureView(
            previewView = previewView,
            overlayColor = overlayColor,
            overlayBorderColor = overlayBorderColor,
            overlayBorderRoundedCornerColor = overlayBorderRoundedCornerColor,
            isCameraPermissionAvailable = hasCamPermission,
            previousMessage = if (imageCount == 0) listMessages.first() else listMessages[imageCount - 1],
            currentMessage = listMessages[imageCount],
            flipTextAnimation = imageCount >= 1
        )

        //Bottom sheet view
        BottomSheetControls(
            isCameraPermissionAvailable = hasCamPermission,
            isFlashLightOn = flashLightOn,
            onFlashLightClick = {
                if (hasCamPermission)
                    flashLightOn = !flashLightOn
            },
            onCaptureClick = {
                if (isProcessing || imageCount >= maxSelectionCount) return@BottomSheetControls

                isProcessing = true
                imageCapture?.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            super.onCaptureSuccess(image)
                            // Convert ImageProxy to ByteArray
                            val byteArray = imageProxyToImageBitmap(image)
                            capturedImages.add(byteArray)
                            // Pass the callback if threshold reached
                            if (imageCount >= maxSelectionCount - 1) {
                                onCompletion(capturedImages)
                            }

                            // Close the ImageProxy to avoid memory leaks
                            image.close()
                            if(imageCount >= maxSelectionCount - 1) {
                                return
                            }
                            if (imageCount < maxSelectionCount - 1) {
                                imageCount++
                            }
                            isProcessing = false
                        }

                        override fun onError(exception: ImageCaptureException) {
                            super.onError(exception)
                            Log.e("CameraX", "Image capture failed: ${exception.message}")
                        }

                    }
                )
            },
            isCaptureEnabled = !isProcessing && imageCount < maxSelectionCount,
            onOpenGalleryPicker = {
                if (!isProcessing && imageCount < maxSelectionCount) pickerState.pickImage(maxCount = maxSelectionCount)
            }
        )
    }

    if (permissionRationalDialog && !hasCamPermission) {
        if (permissionDeniedView == null) {
            PermissionDeniedView(modifier = Modifier.padding(top = 200.dp), onButtonClick = {
                if (cameraPermissionState.status.shouldShowRationale) {
                    cameraPermissionState.launchPermissionRequest()
                } else {
                    launchSetting = true
                }
            })
        } else {
            permissionDeniedView()
        }
    }
}

@Composable
private fun BoxScope.CameraCaptureView(
    previewView: PreviewView,
    overlayColor: Color,
    overlayBorderColor: Color,
    overlayBorderRoundedCornerColor: Color,
    isCameraPermissionAvailable: Boolean,
    previousMessage: String = "Align your ID within the box",
    currentMessage: String = "Align your ID within the box",
    flipTextAnimation: Boolean
) {

    //Android camera native view
    if (isCameraPermissionAvailable) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { androidViewContext ->
                previewView
            }
        )
    }

    //Overlay view
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
fun OverlayView(
    overlayColor: Color = Color(0x88000000),
    overlayBorderColor: Color = Color(0xFF4A4A49),
    overlayBorderRoundedCornerColor: Color = Color.White,
    isCameraPermissionAvailable: Boolean,
    previousMessage: String,
    currentMessage: String,
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
            Image(
                painter = painterResource(R.drawable.gallery_icon),
                contentDescription = "Gallery",
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


            Image(
                painter = painterResource(if (isFlashLightOn) R.drawable.flash_icon_on else R.drawable.flash_icon_off),
                contentDescription = "Flashlight",
                modifier = Modifier
                    .size(32.dp)
                    .clickable {
                        onFlashLightClick()
                    }
            )
        }
    }
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
private fun GalleryPicker(
    maxSelectionCount: Int,
    openImagePicker: MediaPickerState,
    onPhotoPickCompletion: (List<ImageBitmap?>) -> Unit
) {
    val imageArrayList = ArrayList<ImageBitmap?>()
    val coroutineScope = rememberCoroutineScope()
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

private fun imageProxyToImageBitmap(image: ImageProxy): ImageBitmap {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)

    val rotationDegrees = image.imageInfo.rotationDegrees

    // Decode and rotate if needed
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    val rotatedBitmap = if (rotationDegrees != 0) {
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } else {
        bitmap
    }

    // Convert Bitmap → ImageBitmap (Compose)
    return rotatedBitmap.asImageBitmap()
}

@Composable
private fun OpenSettings() {
    val context = LocalContext.current
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    ).also {
        context.startActivity(it)
    }
}