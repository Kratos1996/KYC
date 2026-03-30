package com.chaintech.facematching.service

import com.chaintech.facematching.dataModels.FaceMatchInput
import com.chaintech.facematching.dataModels.FaceMatchingException
import com.chaintech.facematching.dataModels.FaceMatchingResult

/**
 * Interface for face matching operations
 */
interface FaceMatchingService {
    /**
     * Initializes the detector, loading the model and preparing it for inference.
     * This is a long-running operation and must be called from a coroutine.
     */
    suspend fun initialize()

    /**
     * Compares two face images to determine if they belong to the same person
     * param firstImage First face image ByteArray
     * param secondImage Second face image ByteArray
     * param threshold Similarity threshold (0.0 to 1.0). Higher = stricter match
     * @return FaceMatchingResult with match status and similarity score
     * @throws FaceMatchingException if comparison fails
     */
    @Throws(FaceMatchingException::class)
    fun isSamePerson(input: FaceMatchInput): FaceMatchingResult

    companion object {
        /**
         * Creates a new instance of the [FaceMatchingService].
         *
         * @return A new detector instance ready for initialization.
         */
        fun create(): FaceMatchingService {
            return FaceMatchingServiceImpl()
        }
    }
}