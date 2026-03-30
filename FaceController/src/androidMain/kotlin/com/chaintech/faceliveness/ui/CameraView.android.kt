package com.chaintech.faceliveness.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chaintech.facecontroller.R
import com.chaintech.faceliveness.base.FaceConfig
import com.chaintech.faceliveness.util.BorderState
import org.koin.androidx.compose.koinViewModel
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

@Composable
actual fun CameraView(
    faceConfig: FaceConfig,
    onProcessComplete: (image: ImageBitmap, imagePath: String, videoPath: String) -> Unit,
    onClose:()->Unit,
    modifier: Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    // Get ViewModel - use your DI framework here
    val viewModel: FaceDetectionViewModel = koinViewModel()
    val processComplete by viewModel.processDone.collectAsState()
    val brightness by viewModel.brightness.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.faceConfigValue.value = faceConfig
        viewModel.resetForNewSession()
    }
    // Handle completion
    LaunchedEffect(processComplete) {
        if (processComplete) {
            val videoPath = viewModel.finalSelfieVideo.value ?: ""
            val imageBitmap = viewModel.getCurrentBitmap() as? ImageBitmap
            if (imageBitmap != null) {
                viewModel.processDone(true)
                onProcessComplete(imageBitmap, "", videoPath)
            } else {
                viewModel.processDone(false)
            }
        }
    }

    DisposableEffect(Unit) {
        viewModel.initializeFaceDetection()
        onDispose {
            viewModel.cleanup()
        }
    }

    CameraMainUi(modifier, viewModel,lifecycleOwner)
}

@Composable
private fun CameraMainUi(
    modifier: Modifier,
    viewModel: FaceDetectionViewModel,
    lifecycleOwner: LifecycleOwner,
) {
    val borderState by viewModel.borderState.collectAsState()
    val message by viewModel.message.collectAsState()
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    )
    {


        Spacer(modifier = Modifier.height(60.dp))

        // Camera Box
        Box(
            modifier = Modifier
                .size(300.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    val cameraExecutor = Executors.newSingleThreadExecutor()

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            processImageProxyWithBrightness(imageProxy, viewModel)
                        }

                        val videoCapture = VideoCapture.withOutput(
                            Recorder.Builder()
                                .setQualitySelector(
                                    QualitySelector.from(
                                        Quality.HD
                                    )
                                )
                                .build()
                        )

                        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis,
                                videoCapture
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier
                    .size(320.dp)
                    .clip(CircleShape)
            )

            CircularCameraOverlay(
                borderState = borderState,
                modifier = Modifier.size(280.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Brightness Indicator
        Text(
            text = "Brightness: ${viewModel.getBrightnessPercentage()}% - ${viewModel.getBrightnessStatus()}",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Message
        Text(
            text = message,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
    }
}

private fun processImageProxyWithBrightness(
    imageProxy: ImageProxy,
    viewModel: FaceDetectionViewModel
) {
    try {
        val bitmap = CameraConfig.imageProxyToBitmap(imageProxy)
        if (bitmap != null) {
            val brightness = CameraConfig.calculateBrightness(bitmap)
            viewModel.updateBrightness(brightness)

            val adjustedBitmap = when {
                brightness < 0.35f -> CameraConfig.adjustBrightness(bitmap, 1.4f)
                brightness > 0.85f -> CameraConfig.adjustBrightness(bitmap, 0.8f)
                else -> bitmap
            }

            viewModel.setCurrentBitmap(adjustedBitmap.asImageBitmap())
            viewModel.processFrame(adjustedBitmap.asImageBitmap())

            if (adjustedBitmap != bitmap) {
                bitmap.recycle()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        imageProxy.close()
    }
}


