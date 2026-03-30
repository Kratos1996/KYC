package com.chaintech.facialAttributeDetection

import androidx.compose.ui.graphics.ImageBitmap
import com.chaintech.facialAttributeDetection.dataModels.FacialAttributeError
import com.chaintech.facialAttributeDetection.dataModels.FacialAttributesResult
import com.chaintech.facialAttributeDetection.internal.ModelLoader
import com.chaintech.tensorflowrunner.TensorflowLiteRunner
import com.chaintech.tensorflowrunner.processing.ImagePreprocessor
import com.chaintech.tensorflowrunner.processing.ImageSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.exp
import kotlin.math.sqrt

class FacialAttributeDetectionImpl {

    companion object {
        private const val IMAGE_SIZE = 128
    }

    private val imagePreprocessor: ImagePreprocessor = ImagePreprocessor()

    private var tensorflowLiteRunner: TensorflowLiteRunner? = null
    private var modelBytes: ByteArray? = null

    /**
     * Initializes TensorFlow Lite model
     */
    suspend fun initialize() = withContext(Dispatchers.Default) {
        if (tensorflowLiteRunner == null) {
            tensorflowLiteRunner = TensorflowLiteRunner()
            modelBytes = ModelLoader.loadModel()
            tensorflowLiteRunner?.initialize(modelBytes = modelBytes)
            val outputShape = tensorflowLiteRunner?.getOutputShapes()
            println("FacialAttributeDetection -> outputShape: $outputShape")
            println("FacialAttributeDetection -> getOutputNames: ${tensorflowLiteRunner?.getOutputNames()}")
        }
    }

    fun getFacialAttributes(image: ImageBitmap): FacialAttributesResult {
        val currentInterpreter = tensorflowLiteRunner
            ?: throw FacialAttributeError.InterpreterNotInitialized()

        // 1. Preprocess
        val rgbBytes = preprocessImage(image)

        currentInterpreter.runInference(inputData = rgbBytes)

        // 3. Collect outputs
        fun processOutput(name: String): FloatArray {
            val tensorData = currentInterpreter.getOutputByName(name)
                ?: throw FacialAttributeError.OutputNotFound(name)

            return dequantizeUInt8(
                tensorData.data,
                tensorData.scale,
                tensorData.zeroPoint
            )
        }

        // Embeddings
        val idFeature = processOutput("id_feature").l2Normalized()
        val livenessFeature = processOutput("liveness_feature")

        // 2-class heads
        fun probs(name: String): FloatArray {
            val vec = processOutput(name)
            return softmax(vec)
        }

        val eyeProbs = probs("eye_closeness")
        val eyePrediction = eyeProbs.indexOfMax()

        val glassesProbs = probs("glasses")
        val glassesPrediction = glassesProbs.indexOfMax()

        val maskProbs = probs("mask")
        val maskPrediction = maskProbs.indexOfMax()

        val sunglassesProbs = probs("sunglasses")
        val sunglassesPrediction = sunglassesProbs.indexOfMax()

        val facialAttributesResult = FacialAttributesResult(
            idFeature = idFeature,
            livenessFeature = livenessFeature,
            eyeCloseness = FacialAttributesResult.PredictionResult(
                confidence = (eyeProbs.maxOrNull() ?: 0f) * 100,
                prediction = if (eyePrediction == 1) "Closed" else "Open"
            ),
            glasses = FacialAttributesResult.PredictionResult(
                confidence = (glassesProbs.maxOrNull() ?: 0f) * 100,
                prediction = glassesPrediction == 1
            ),
            mask = FacialAttributesResult.PredictionResult(
                confidence = (maskProbs.maxOrNull() ?: 0f) * 100,
                prediction = maskPrediction == 1
            ),
            sunglasses = FacialAttributesResult.PredictionResult(
                confidence = (sunglassesProbs.maxOrNull() ?: 0f) * 100,
                prediction = sunglassesPrediction == 1
            )
        )

        return facialAttributesResult
    }

    private fun preprocessImage(image: ImageBitmap): ByteArray {
        val resizeImageBitmap =
            imagePreprocessor.resizeImage(image, ImageSize(IMAGE_SIZE, IMAGE_SIZE))

        return resizeImageBitmap
    }

    private fun FloatArray.indexOfMax(): Int {
        return this.indices.maxByOrNull { this[it] } ?: 0
    }

    /**
     * Cleans up resources (interpreter and decrypted model file)
     */
    fun dispose() {
        try {
            tensorflowLiteRunner?.dispose()
            tensorflowLiteRunner = null
            modelBytes = null
            ModelLoader.clearCache()
        } catch (e: Exception) {
            // Log but don't throw - cleanup is best effort
            println("Warning: Failed to cleanup resources: ${e.message}")
        }
    }

    /**
     * Apply softmax to convert logits to probabilities
     */
    private fun softmax(values: FloatArray): FloatArray {
        if (values.isEmpty()) return floatArrayOf()

        val maxVal = values.maxOrNull() ?: 0f
        val exps = values.map { exp(it - maxVal) }
        val sum = exps.sum()

        return exps.map { it / sum }.toFloatArray()
    }

    /**
     * Dequantize UInt8 tensor data to float values
     */
    private fun dequantizeUInt8(
        data: ByteArray,
        scale: Float,
        zeroPoint: Int
    ): FloatArray {
        return FloatArray(data.size) { i ->
            (data[i].toUByte().toInt() - zeroPoint) * scale
        }
    }

    /**
     * L2 normalize a vector
     */
    private fun FloatArray.l2Normalized(): FloatArray {
        val norm = sqrt(this.fold(0f) { acc, value -> acc + value * value })
        return if (norm > 0f) {
            FloatArray(this.size) { i -> this[i] / norm }
        } else {
            this
        }
    }
}