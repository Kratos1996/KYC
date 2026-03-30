package com.chaintech.faceantispoofing.service

import com.chaintech.faceantispoofing.dataModels.AntiSpoofingResult
import com.chaintech.faceantispoofing.dataModels.FaceAntiSpoofingException
import com.chaintech.faceantispoofing.dataModels.FaceAntiSpoofingInput

/**
 * Interface for face matching operations
 */
interface FaceAntiSpoofingService {
    /**
     * Initializes the detector, loading the model and preparing it for inference.
     * This is a long-running operation and must be called from a coroutine.
     */
    suspend fun initialize()

    @Throws(FaceAntiSpoofingException::class)
    fun isSpoofedFaceDetected(input: FaceAntiSpoofingInput): AntiSpoofingResult

    fun dispose()
    companion object {
        /**
         * Creates a new instance of the [FaceAntiSpoofingService].
         *
         * @return A new detector instance ready for initialization.
         */
        fun create(): FaceAntiSpoofingService {
            return FaceAntiSpoofingServiceImpl()
        }
    }
}