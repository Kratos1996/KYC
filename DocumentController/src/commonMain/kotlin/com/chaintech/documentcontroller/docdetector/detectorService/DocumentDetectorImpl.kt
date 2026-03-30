package com.chaintech.documentcontroller.docdetector.detectorService

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.chaintech.documentcontroller.docdetector.dataModels.BoundingBox
import com.chaintech.documentcontroller.docdetector.internal.ModelLoader
import com.chaintech.documentcontroller.docdetector.processing.DetectionPostProcessor
import com.chaintech.tensorflowrunner.TensorflowLiteRunner
import com.chaintech.tensorflowrunner.processing.ImagePreprocessor
import com.chaintech.tensorflowrunner.processing.ImageSize
import com.chaintech.tensorflowrunner.utils.DataConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class DocumentDetectorImpl : DocumentDetectorService {

    companion object {
        private const val MODEL_INPUT_SIZE = 640
    }

    private var tensorflowLiteRunner: TensorflowLiteRunner? = null
    private var modelBytes: ByteArray? = null
    private val postProcessor = DetectionPostProcessor()
    private val preProcessor = ImagePreprocessor()

    override suspend fun initialize() = withContext(Dispatchers.Default) {
        if (tensorflowLiteRunner == null) {
            tensorflowLiteRunner = TensorflowLiteRunner()
            modelBytes = ModelLoader.loadModel()
            tensorflowLiteRunner?.initialize(modelBytes = modelBytes)
            val outputShape = tensorflowLiteRunner?.getOutputShapes()?.get(0)

            postProcessor.initialize(
                numChannel = outputShape?.get(1) ?: 0,
                numElements = outputShape?.get(2) ?: 0
            )
        }
    }

    override suspend fun detectDocument(
        image: ImageBitmap,
        imageWidth: Int,
        imageHeight: Int
    ): DocumentDetectorData = withContext(Dispatchers.Default) {

        // Ensure initialized
        if (tensorflowLiteRunner == null || modelBytes == null) {
            initialize()
        }
        val imageData =  image.toRGBBytes()

        // Preprocess image to model input format
        val preprocessedData = preProcessor.preprocessImage(
            imageData = imageData,
            imageSize = ImageSize(imageWidth, imageHeight),
            targetSize = ImageSize(MODEL_INPUT_SIZE, MODEL_INPUT_SIZE)
        )

        // Convert preprocessed float array to ByteArray for platform-specific code
        val inputBytes = DataConverter.floatArrayToByteArray(preprocessedData)

        // Run inference (platform-specific)
        val outputBytes = tensorflowLiteRunner?.runInference(inputData = inputBytes)
            ?: throw IllegalStateException("TFLite runner not initialized")

        // Convert output back to FloatArray
        val outputData = DataConverter.byteArrayToFloatArray(outputBytes.output)

        // Process output to get bounding boxes
        val detections = postProcessor.processOutput(outputData, imageWidth, imageHeight)

        //cropped imageBitmap
        if (detections.isNotEmpty()) {
            val croppedImage = cropImageBitmap(source = image, box = detections.first().boundingBox)
            val data =
                DocumentDetectorData(dectedListData = detections, croppedBitmap = croppedImage)
            return@withContext data
        } else {
            val data =
                DocumentDetectorData(dectedListData = detections, croppedBitmap = image)
            return@withContext data
        }
    }

    override fun dispose() {
        tensorflowLiteRunner?.dispose()
        tensorflowLiteRunner = null
        modelBytes = null
        ModelLoader.clearCache()
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
}
