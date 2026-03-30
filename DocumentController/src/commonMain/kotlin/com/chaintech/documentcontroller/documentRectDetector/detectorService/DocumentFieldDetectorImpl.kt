package com.chaintech.documentcontroller.documentRectDetector.detectorService

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import com.chaintech.documentcontroller.documentRectDetector.internal.ModelLoader
import com.chaintech.documentcontroller.documentRectDetector.dataModels.DocumentRegionRect
import com.chaintech.documentcontroller.documentRectDetector.dataModels.SizeF
import com.chaintech.documentcontroller.documentRectDetector.processing.DetectionPostProcessor
import com.chaintech.tensorflowrunner.TensorflowLiteRunner
import com.chaintech.tensorflowrunner.processing.ImagePreprocessor
import com.chaintech.tensorflowrunner.processing.ImageSize
import com.chaintech.tensorflowrunner.utils.DataConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Internal implementation of the DocumentFieldDetector.
 * This class is not visible to the host app.
 */
internal class DocumentFieldDetectorImpl : DocumentFieldDetectorService {

    companion object {
        private const val MODEL_INPUT_SIZE = 640 // Adjust based on your model
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

            val inputShape = tensorflowLiteRunner?.getInputShape()
            val outputShape = tensorflowLiteRunner?.getOutputShapes()?.get(0)

            postProcessor.initialize(
                numChannel = outputShape?.get(1) ?: 0,
                numElements = outputShape?.get(2) ?: 0
            )
        }
    }

    override suspend fun detectFields(
        image: ImageBitmap,
        imageWidth: Int,
        imageHeight: Int
    ): List<DocumentRegionRect> = withContext(Dispatchers.Default) {
        // Ensure initialized
        if (tensorflowLiteRunner == null || modelBytes == null) {
            initialize()
        }

        // Preprocess image to model input format
        val preprocessedData = preProcessor.preprocessImage(
            imageData = image.toRGBBytes(),
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
        val detections = postProcessor.processOutput(outputData)

        val finalResult = postProcessor.prepareDetectionResults(
            image = image,
            boundingBoxes = detections,
            processedImageSize = SizeF(MODEL_INPUT_SIZE.toFloat(), MODEL_INPUT_SIZE.toFloat()),
            originalImageSize = SizeF(imageWidth.toFloat(), imageHeight.toFloat())
        )

        // Scale results back to original image size
        return@withContext finalResult
    }

    override fun dispose() {
        tensorflowLiteRunner?.dispose()
        tensorflowLiteRunner = null
        modelBytes = null
        ModelLoader.clearCache()
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