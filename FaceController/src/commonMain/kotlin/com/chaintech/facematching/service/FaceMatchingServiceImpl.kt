package com.chaintech.facematching.service

import com.chaintech.facematching.dataModels.FaceImageSize
import com.chaintech.facematching.dataModels.FaceMatchInput
import com.chaintech.facematching.dataModels.FaceMatchingException
import com.chaintech.facematching.dataModels.FaceMatchingResult
import com.chaintech.facematching.internal.ModelLoader
import com.chaintech.facematching.processing.EmbeddingPostProcessor
import com.chaintech.tensorflowrunner.utils.DataConverter
import com.chaintech.tensorflowrunner.TensorflowLiteRunner
import com.chaintech.tensorflowrunner.processing.ImagePreprocessor
import com.chaintech.tensorflowrunner.processing.ImageSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Main face matching implementation
 * Image Processing → ML Inference → Similarity Comparison
 */
class FaceMatchingServiceImpl : FaceMatchingService {

    companion object {
        /** Input image size for FaceNet model (160x160) */
        private const val IMG_SIZE = 160

        /** Output embedding dimension */
        private const val EMBEDDING_DIM = 512
    }

    private val imagePreprocessor: ImagePreprocessor = ImagePreprocessor()
    private val embeddingProcessor: EmbeddingPostProcessor = EmbeddingPostProcessor()

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
    override fun isSamePerson(input: FaceMatchInput): FaceMatchingResult {
        return try {
            // Generate embeddings for both images
            val firstEmbedding = getFaceEmbedding(input.firstImage, input.firstFaceImageSize)
                ?: throw FaceMatchingException.ImagePreprocessingError(
                    "Failed to process first image. Please verify it's a valid face image."
                )

            val secondEmbedding = getFaceEmbedding(input.secondImage, input.secondFaceImageSize)
                ?: throw FaceMatchingException.ImagePreprocessingError(
                    "Failed to process second image. Please verify it's a valid face image."
                )

            // Compare embeddings using cosine similarity
            embeddingProcessor.areFacesSimilar(
                embedding1 = firstEmbedding,
                embedding2 = secondEmbedding,
                threshold = input.threshold
            )
        } finally {
            println("FaceMatching -> isSamePerson - cleanupResources")
            // Always cleanup resources
            cleanupResources()
        }
    }

    /**
     * Generates face embedding from image
     * Pipeline: Image → Preprocess → Model → Embedding
     */
    private fun getFaceEmbedding(image: ByteArray, imageSize: FaceImageSize): FloatArray? {
        val currentInterpreter = tensorflowLiteRunner
            ?: throw FaceMatchingException.ModelNotInitialized()
        return try {
            // Step 1: Preprocess image (resize + normalize)
            val preprocessedData = imagePreprocessor.preprocessImage(image,
                ImageSize(imageSize.width, imageSize.height),
                ImageSize(IMG_SIZE, IMG_SIZE))

            // Step 2: Validate input size (160 * 160 * 3 = 76,800)
            val expectedSize = IMG_SIZE * IMG_SIZE * 3
            if (preprocessedData.size != expectedSize) {
                throw FaceMatchingException.EmbeddingGenerationError(
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
                throw FaceMatchingException.EmbeddingGenerationError(
                    "Invalid embedding size. Expected $EMBEDDING_DIM, got ${embedding.size}"
                )
            }
            embedding
        } catch (e: FaceMatchingException) {
            throw e
        } catch (e: Exception) {
            throw FaceMatchingException.EmbeddingGenerationError(
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