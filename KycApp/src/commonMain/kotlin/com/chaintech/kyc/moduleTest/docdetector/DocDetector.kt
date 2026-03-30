package com.chaintech.kyc.moduleTest.docdetector

import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.chaintech.documentcontroller.docdetector.DocumentDetectorSDK
import com.chaintech.documentcontroller.docdetector.dataModels.BoundingBox
import com.chaintech.kyc.moduleTest.docFieldDetector.toRGBBytes
import kotlinx.coroutines.launch
import kyc.kycapp.generated.resources.Res
import kyc.kycapp.generated.resources.TempCropImageTest_1
import org.jetbrains.compose.resources.imageResource

@Composable
fun DocumentDetectorDemo() {
    val scope = rememberCoroutineScope()
    val detector = remember { DocumentDetectorSDK.createDetectorService() }
    val image = imageResource(Res.drawable.TempCropImageTest_1)

    val isCropButtonTap = remember { mutableStateOf(false) }
    var cropImage by remember { mutableStateOf<ImageBitmap?>(null) }

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
                    val width = image.width
                    val height = image.height
                    println("Width: $width, Height: $height")

                    val imageBytesArray = image.toRGBBytes()

                    // Initialize detector (loads model from resources)
                    detector.initialize()

                    // Detect fields
                    val results = detector.detectDocument(image, width, height)

                    if (results.dectedListData.isNotEmpty()) {
                        println("***************** Document Field into *****************")
                        println("\n")
                        // Process results
                        results.dectedListData.forEach { region ->
                            println("***************** ${region.className} *****************")
                            println("classId : ${region.classId}")
                            println("ClassName ${region.className}")
                            println("boundingBox : ${region.boundingBox}")
                            println("confidence : ${region.confidence}")
                            println("***************** ***************** *******************")
                            println("\n")
                        }
                        println("\n")
                        println("***************** ***************** *******************")

                        cropImage = cropImageBitmap(image, results.dectedListData.first().boundingBox)
                        isCropButtonTap.value = true
                    }

                    // Clean up when done
                    detector.dispose()
                } catch (e: Exception) {
                    println("❌ Unexpected error: ${e.message}")
                }
            }
        }) {
            Text("Document Field Detector Demo")
        }

        if (isCropButtonTap.value) {
            CroppedImageView(image,
                modifier = Modifier
                    .size(width = 200.dp, height = 200.dp)
                    .padding(horizontal = 5.dp)
            )

            cropImage?.let {
                CroppedImageView(it,
                    modifier = Modifier
                        .size(width = 200.dp, height = 200.dp)
                        .padding(horizontal = 5.dp)
                )
            }
        }
    }
}

private fun cropImageBitmap(source: ImageBitmap, box: BoundingBox): ImageBitmap {
    val left = box.x.toInt().coerceAtLeast(0)
    val top = box.y.toInt().coerceAtLeast(0)
    val width = box.width.toInt().coerceAtMost(source.width - left)
    val height = box.height.toInt().coerceAtMost(source.height - top)

    val cropped = ImageBitmap(width, height)
    val canvas = Canvas(cropped)
    val paint = Paint()

    canvas.drawImageRect(
        image = source,
        srcOffset = IntOffset(left, top),
        srcSize = IntSize(width, height),
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(width, height),
        paint = paint
    )

    return cropped
}

@Composable
fun CroppedImageView(image: ImageBitmap, modifier: Modifier) {
    Image(
        bitmap = image,
        contentDescription = "Cropped document",
        modifier = modifier
    )
}