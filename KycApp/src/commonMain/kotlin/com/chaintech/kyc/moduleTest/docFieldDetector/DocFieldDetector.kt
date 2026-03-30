package com.chaintech.kyc.moduleTest.docFieldDetector

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.chaintech.documentcontroller.docdetector.dataModels.BoundingBox
import com.chaintech.documentcontroller.documentRectDetector.DocumentFieldDetectorSDK
import com.chaintech.googlemlkitocr.createTextRecognizer
import kotlinx.coroutines.launch
import kyc.kycapp.generated.resources.Res
import kyc.kycapp.generated.resources.TempPancard
import org.jetbrains.compose.resources.imageResource

@Composable
fun DocumentFieldDetectorDemo() {
    val scope = rememberCoroutineScope()
    val detector = remember { DocumentFieldDetectorSDK.createDetectorService() }
    val image = imageResource(Res.drawable.TempPancard)
    val textRecognizer = createTextRecognizer()

    Button(onClick = {
        scope.launch {
            try {
                val width = image.width
                val height = image.height
                println("Width: $width, Height: $height")

                // Initialize detector (loads model from resources)
                detector.initialize()

                // Detect fields
                val results = detector.detectFields(image, width, height)

                println("***************** Document Field into *****************")
                println("\n")
                // Process results
                results.forEach { region ->
                    val cropImage = cropImageBitmap(image, box = region.boundingBox)
                    val ocrText = textRecognizer.processImage(cropImage)

                    println("***************** ${region.className} *****************")
                    println("classId : ${region.classId}")
                    println("ClassName ${region.className}")
                    println("OcrText ${ocrText}")
                    println("boundingBox : ${region.boundingBox}")
                    println("confidence : ${region.confidence}")
                    println("***************** ***************** *******************")
                    println("\n")
                }
                println("\n")
                println("***************** ***************** *******************")
                // Clean up when done
                detector.dispose()

            } catch (e: Exception) {
                println("❌ Unexpected error: ${e.message}")
            }
        }
    }) {
        Text("Document Field Detector Demo")
    }
}

fun cropImageBitmap(source: ImageBitmap, box: BoundingBox): ImageBitmap {
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

//  Using Color API correctly
fun ImageBitmap.toRGBBytes(): ByteArray {
    val pixelMap = this.toPixelMap()
    val width = this.width
    val height = this.height
    val bytes = ByteArray(width * height * 3)

    var index = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            val color = pixelMap[x, y] // Convert Int to Color
            bytes[index++] = (color.red * 255).toInt().toByte()   // Red
            bytes[index++] = (color.green * 255).toInt().toByte() // Green
            bytes[index++] = (color.blue * 255).toInt().toByte()  // Blue
        }
    }

    return bytes
}

// ✅ ARGB version
fun ImageBitmap.toARGBBytes(): ByteArray {
    val pixelMap = this.toPixelMap()
    val width = this.width
    val height = this.height
    val bytes = ByteArray(width * height * 4)

    var index = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            val color = pixelMap[x, y]
            bytes[index++] = (color.alpha * 255).toInt().toByte() // Alpha
            bytes[index++] = (color.red * 255).toInt().toByte()   // Red
            bytes[index++] = (color.green * 255).toInt().toByte() // Green
            bytes[index++] = (color.blue * 255).toInt().toByte()  // Blue
        }
    }

    return bytes
}

// ✅ For TensorFlow - Normalized float array [0.0 - 1.0]
fun ImageBitmap.toFloatArray(): FloatArray {
    val pixelMap = this.toPixelMap()
    val width = this.width
    val height = this.height
    val floats = FloatArray(width * height * 3)

    var index = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            val color = pixelMap[x, y]
            floats[index++] = color.red   // Already 0.0 - 1.0
            floats[index++] = color.green // Already 0.0 - 1.0
            floats[index++] = color.blue  // Already 0.0 - 1.0
        }
    }

    return floats
}