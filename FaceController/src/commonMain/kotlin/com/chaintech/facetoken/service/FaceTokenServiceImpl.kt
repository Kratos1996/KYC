package com.chaintech.facetoken.service

import androidx.compose.ui.graphics.ImageBitmap
import com.chaintech.facetoken.dataModels.FaceTokenException
import com.chaintech.facetoken.internal.ModelLoader
import com.chaintech.tensorflowrunner.TensorflowLiteRunner
import com.chaintech.tensorflowrunner.processing.ImagePreprocessor
import com.chaintech.tensorflowrunner.processing.ImageSize
import com.chaintech.tensorflowrunner.processing.toRGBBytes
import com.chaintech.tensorflowrunner.utils.DataConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FaceTokenServiceImpl : FaceTokenService {

    companion object Companion {
        /** Input image size for FaceNet model (160x160) */
        private const val IMG_SIZE = 160

        /** Output embedding dimension */
        private const val EMBEDDING_DIM = 512
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
    override fun getFaceToken(imageBitmap: ImageBitmap): FloatArray {
        try {
            // Generate embeddings for both images
            val firstEmbedding = getFaceEmbedding(imageBitmap)
                ?: throw FaceTokenException.ImagePreprocessingError(
                    "Failed to process first image. Please verify it's a valid face image."
                )

            return firstEmbedding
        } finally {
            // Always cleanup resources
            cleanupResources()
        }
    }

    private fun getFaceEmbedding(image: ImageBitmap): FloatArray? {
        val currentInterpreter = tensorflowLiteRunner
            ?: throw FaceTokenException.ModelNotInitialized()
        return try {
            // Step 1: Preprocess image (resize + normalize)
            val preprocessedData = imagePreprocessor.preprocessImage(image.toRGBBytes(),
                ImageSize(image.width, image.height),
                ImageSize(IMG_SIZE, IMG_SIZE))

            // Step 2: Validate input size (160 * 160 * 3 = 76,800)
            val expectedSize = IMG_SIZE * IMG_SIZE * 3
            if (preprocessedData.size != expectedSize) {
                throw FaceTokenException.EmbeddingGenerationError(
                    "Invalid preprocessed data size. Expected $expectedSize, got ${preprocessedData.size}"
                )
            }

            // Convert preprocessed float array to ByteArray for platform-specific code
            val inputBytes = DataConverter.floatArrayToByteArray(preprocessedData)

            // Step 3: Run inference
            val interfaceResult = currentInterpreter.runInference(inputData = inputBytes)

            // Step 5: Get output embedding
            val embedding = DataConverter.byteArrayToFloatArray(interfaceResult.output)

            // Step 6: Validate output size
            if (embedding.size != EMBEDDING_DIM) {
                throw FaceTokenException.EmbeddingGenerationError(
                    "Invalid embedding size. Expected $EMBEDDING_DIM, got ${embedding.size}"
                )
            }
            embedding
        } catch (e: FaceTokenException) {
            throw e
        } catch (e: Exception) {
            throw FaceTokenException.EmbeddingGenerationError(
                "Failed to generate embedding: ${e.message}"
            )
        }
    }

    /**
     * Cleans up resources (interpreter and decrypted model file)
     */
    private fun cleanupResources() {
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