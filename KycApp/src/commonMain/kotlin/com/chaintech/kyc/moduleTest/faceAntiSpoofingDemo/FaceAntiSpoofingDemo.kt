package com.chaintech.kyc.moduleTest.faceAntiSpoofingDemo

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.chaintech.faceantispoofing.FaceAntiSpoofingSDK
import com.chaintech.faceantispoofing.dataModels.FaceAntiSpoofingInput
import com.chaintech.kyc.moduleTest.docFieldDetector.toRGBBytes
import kotlinx.coroutines.launch
import kyc.kycapp.generated.resources.Res
import kyc.kycapp.generated.resources.TempElonMusk1
import org.jetbrains.compose.resources.imageResource

@Composable
fun FaceAntiSpoofingDemo() {
    val scope = rememberCoroutineScope()
    val faceAntiSpoofing = remember { FaceAntiSpoofingSDK.createDetectorService() }
    val personImage1 = imageResource(Res.drawable.TempElonMusk1)

    Button(onClick = {
        scope.launch {
            try {
                val person1BytesArray = personImage1.toRGBBytes()

                // Initialize detector (loads model from resources)
                faceAntiSpoofing.initialize()

                val results = faceAntiSpoofing.isSpoofedFaceDetected(
                    input = FaceAntiSpoofingInput(
                        image = person1BytesArray,
                        width = personImage1.width,
                        height = personImage1.height
                    )
                )
                println("***************** Document Field into *****************")
                println("Face Matching Result: $results")
                println("***************** ***************** *******************")

                faceAntiSpoofing.dispose()
            } catch (e: Exception) {
                println("❌ Face Matching Unexpected error: ${e.message}")
            }
        }
    }) {
        Text("FaceAntiSpoofing Demo")
    }
}