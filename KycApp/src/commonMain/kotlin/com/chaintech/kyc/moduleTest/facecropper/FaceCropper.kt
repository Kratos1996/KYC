package com.chaintech.kyc.moduleTest.facecropper

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.chaintech.facecropper.ErrorCode
import com.chaintech.facecropper.FaceCropper
import com.chaintech.facecropper.FaceCropperException
import com.chaintech.kyc.moduleTest.docdetector.CroppedImageView
import kotlinx.coroutines.launch
import kyc.kycapp.generated.resources.Res
import kyc.kycapp.generated.resources.TempTomCruise1
import org.jetbrains.compose.resources.imageResource
import org.koin.compose.koinInject

@Composable
fun FaceCropperDemo() {
    val scope = rememberCoroutineScope()
    val faceCropper = koinInject<FaceCropper>()

    var croppedFace by remember { mutableStateOf<ImageBitmap?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val personImage1 = imageResource(Res.drawable.TempTomCruise1)

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .safeContentPadding()
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(onClick = {
            scope.launch {
                try {
                    faceCropper.getCroppedFaceUsingBitmap(image = personImage1, expandRatio = 1.2f).fold(
                        onSuccess = { result ->
                            croppedFace = result.bitmap
                            println("confidence - ${result.confidence}")
                            println("boundingBox - ${result.boundingBox}")
                            faceCropper.close()
                        },
                        onFailure = { exception ->
                            errorMessage =
                                when ((exception as? FaceCropperException)?.errorCode) {
                                    ErrorCode.NO_FACE -> "No face detected in the image"
                                    ErrorCode.MULTIPLE_FACES -> "Multiple faces detected. Please ensure only one face is visible"
                                    ErrorCode.DETECTOR_CLOSED -> "Face detector is closed"
                                    else -> "Failed to detect face: ${exception.message}"
                                }
                            println("Error Message:- $errorMessage")
                            faceCropper.close()
                        }
                    )
                } catch (e: Exception) {
                    println("❌ Face Matching Unexpected error: ${e.message}")
                }
            }
        }) {
            Text("FaceAntiSpoofing Demo")
        }

        croppedFace?.let {
            CroppedImageView(
                it,
                modifier = Modifier
                    .size(width = 200.dp, height = 200.dp)
                    .padding(horizontal = 5.dp)
            )
        }
    }
}