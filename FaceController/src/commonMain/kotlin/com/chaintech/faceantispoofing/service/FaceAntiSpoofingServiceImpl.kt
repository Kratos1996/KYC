package com.chaintech.faceantispoofing.service

import com.chaintech.faceantispoofing.dataModels.AntiSpoofingResult
import com.chaintech.faceantispoofing.dataModels.FaceAntiSpoofingException
import com.chaintech.faceantispoofing.dataModels.FaceAntiSpoofingInput
import com.chaintech.faceantispoofing.internal.ModelLoader
import com.chaintech.faceantispoofing.untils.toPlainString
import com.chaintech.tensorflowrunner.TensorflowLiteRunner
import com.chaintech.tensorflowrunner.processing.ImagePreprocessor
import com.chaintech.tensorflowrunner.processing.ImageSize
import com.chaintech.tensorflowrunner.utils.DataConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Main face matching implementation
 * Image Processing → ML Inference → Similarity Comparison
 */
class FaceAntiSpoofingServiceImpl : FaceAntiSpoofingService {

    companion object {
        /** Input image size for FaceAntiSpoofing model (256x256) */
        private const val IMAGE_SIZE = 256
        // Set a threshold, a value greater than this is considered an attack
        private const val ANTI_SPOOFING_THRESHOLD = 0.2
    }

    private val imagePreprocessor: ImagePreprocessor = ImagePreprocessor()

    private var tensorflowLiteRunner: TensorflowLiteRunner? = null
    private var modelBytes: ByteArray? = null

    /**
     * Initializes TensorFlow Lite model
     */
    override suspend fun initialize() = withContext(Dispatchers.Default) {
        if (tensorflowLiteRunner == null) {
            tensorflowLiteRunner = TensorflowLiteRunner()
            modelBytes = ModelLoader.loadModel()
            tensorflowLiteRunner?.initialize(modelBytes = modelBytes)
        }
    }

    /**
     * Compares two face images
     */
    override fun isSpoofedFaceDetected(input: FaceAntiSpoofingInput): AntiSpoofingResult {
        val antiSpoofingScore = antiSpoofing(input)
        // Set a threshold, a value greater than this is considered an attack
        val isSpoofed = antiSpoofingScore > ANTI_SPOOFING_THRESHOLD
        val formattedScore = antiSpoofingScore.toPlainString()
        println("FaceAntiSpoofing -> isSpoofedFaceDetected - formattedScore: $formattedScore")
        return AntiSpoofingResult(isSpoofed, formattedScore)
    }

    private fun antiSpoofing(input: FaceAntiSpoofingInput): Float {
        val currentInterpreter = tensorflowLiteRunner
            ?: throw FaceAntiSpoofingException.ModelNotInitialized()

        // Step 1: Preprocess image (resize + normalize)
        val preprocessedData = imagePreprocessor.preprocessImage(input.image,
            ImageSize(input.width, input.height),
            ImageSize(IMAGE_SIZE, IMAGE_SIZE)
        )

        // Step 2: Validate input size
        val expectedSize = IMAGE_SIZE * IMAGE_SIZE * 3
        if (preprocessedData.size != expectedSize) {
            throw FaceAntiSpoofingException.InvalidInput(
                "Invalid preprocessed data size. Expected $expectedSize, got ${preprocessedData.size}"
            )
        }

        // Convert preprocessed float array to ByteArray for platform-specific code
        val inputBytes = DataConverter.floatArrayToByteArray(preprocessedData)

        // Step 3: Run inference
        val interfaceResult = currentInterpreter.runInference(inputData = inputBytes)

        // Step 4: Get output
        val classPrediction = DataConverter.byteArrayToFloatArray(interfaceResult.outputs[0])
        val leafNodeMask = DataConverter.byteArrayToFloatArray(interfaceResult.outputs[1])

        val antiSpoofingScore = classPrediction.zip(leafNodeMask).fold(0.0f) { accumulator, pair ->
            accumulator + abs(pair.first) * pair.second
        }

        return antiSpoofingScore
    }

    /**
     * Cleans up resources (interpreter and decrypted model file)
     */
    override fun dispose() {
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
}