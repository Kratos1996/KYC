package com.chaintech.kyc.moduleTest.faceMatchingDemo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chaintech.facematching.FaceMatchingSDK
import com.chaintech.facematching.dataModels.FaceImageSize
import com.chaintech.facematching.dataModels.FaceMatchInput
import com.chaintech.kyc.moduleTest.docFieldDetector.toRGBBytes
import com.chaintech.kyc.moduleTest.docdetector.CroppedImageView
import kotlinx.coroutines.launch
import kyc.kycapp.generated.resources.Res
import kyc.kycapp.generated.resources.TempCropImageTest_1
import org.jetbrains.compose.resources.imageResource

@Composable
fun FaceMatchingDemo() {
    val scope = rememberCoroutineScope()
    val faceMatching = remember { FaceMatchingSDK.createDetectorService() }
    val personImage1 = imageResource(Res.drawable.TempCropImageTest_1)
    val personImage2 = imageResource(Res.drawable.TempCropImageTest_1)

    val isButtonTap = remember { mutableStateOf(false) }
    val score = remember { mutableStateOf(0.0) }

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
                    val person1BytesArray = personImage1.toRGBBytes()
                    val person2BytesArray = personImage2.toRGBBytes()

                    // Initialize detector (loads model from resources)
                    faceMatching.initialize()

                    // Detect Face Matching Result
                    val results = faceMatching.isSamePerson(
                        input = FaceMatchInput(
                            firstImage = person1BytesArray,
                            secondImage = person2BytesArray,
                            firstFaceImageSize = FaceImageSize(
                                personImage1.width,
                                personImage1.height
                            ),
                            secondFaceImageSize = FaceImageSize(
                                personImage2.width,
                                personImage2.height
                            )
                        )
                    )

                    println("***************** Document Field into *****************")
                    println("Face Matching Result: $results")
                    println("***************** ***************** *******************")

                    isButtonTap.value = true
                    score.value = results.similarity.toDouble()
                } catch (e: Exception) {
                    println("❌ Face Matching Unexpected error: ${e.message}")
                }
            }
        }) {
            Text("Face Matching Demo")
        }

        if (isButtonTap.value) {
            Row {
                CroppedImageView(
                    personImage1,
                    modifier = Modifier
                        .size(width = 200.dp, height = 200.dp)
                        .padding(horizontal = 5.dp)
                )

                CroppedImageView(
                    personImage2,
                    modifier = Modifier
                        .size(width = 200.dp, height = 200.dp)
                        .padding(horizontal = 5.dp)
                )
            }

            Text("Score:  ${score.value}")
        }
    }
}