package com.chaintech.facematching.processing

import com.chaintech.facematching.dataModels.FaceMatchingException
import com.chaintech.facematching.dataModels.FaceMatchingResult
import kotlin.math.sqrt

/**
 * Face embedding comparison using cosine similarity
 */
internal class EmbeddingPostProcessor {

    /**
     * Compares two face embeddings
     * @param embedding1 First face embedding vector
     * @param embedding2 Second face embedding vector
     * @param threshold Similarity threshold (0.0 to 1.0)
     * @return FaceMatchingResult with similarity score and match status
     */
    fun areFacesSimilar(
        embedding1: FloatArray,
        embedding2: FloatArray,
        threshold: Float = 0.7f
    ): FaceMatchingResult {
        validateEmbeddings(embedding1, embedding2)
        validateThreshold(threshold)

        val similarity = calculateCosineSimilarity(embedding1, embedding2)

        // Log for debugging
        println("FaceMatching → CosineSimilarity: $similarity, Threshold: $threshold")

        return FaceMatchingResult(
            isSimilar = similarity >= threshold,
            similarity = similarity
        )
    }

    /**
     * Calculates cosine similarity: cos(θ) = (A · B) / (||A|| × ||B||)
     */
    private fun calculateCosineSimilarity(
        embedding1: FloatArray,
        embedding2: FloatArray
    ): Float {
        var dotProduct = 0f
        var magnitude1 = 0f
        var magnitude2 = 0f

        for (i in embedding1.indices) {
            val e1 = embedding1[i]
            val e2 = embedding2[i]

            dotProduct += e1 * e2
            magnitude1 += e1 * e1
            magnitude2 += e2 * e2
        }

        magnitude1 = sqrt(magnitude1)
        magnitude2 = sqrt(magnitude2)

        return if (magnitude1 > 0f && magnitude2 > 0f) {
            dotProduct / (magnitude1 * magnitude2)
        } else {
            0f
        }
    }

    private fun validateEmbeddings(embedding1: FloatArray, embedding2: FloatArray) {
        if (embedding1.size != embedding2.size) {
            throw FaceMatchingException.InvalidInput(
                "Embeddings must have same size. Got ${embedding1.size} and ${embedding2.size}"
            )
        }

        if (embedding1.isEmpty()) {
            throw FaceMatchingException.InvalidInput("Embeddings cannot be empty")
        }
    }

    private fun validateThreshold(threshold: Float) {
        if (threshold !in 0.0f..1.0f) {
            throw FaceMatchingException.InvalidInput(
                "Threshold must be between 0.0 and 1.0. Got $threshold"
            )
        }
    }
}