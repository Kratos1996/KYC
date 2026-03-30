package com.chaintech.facematching

import com.chaintech.facematching.service.FaceMatchingService


/**
 * Singleton object that provides a factory method for creating instances
 * of the FaceMatchingService.
 *
 * This acts as the main entry point to initialize and access the
 * document field detection functionality within the SDK.
 */
object FaceMatchingSDK {
    /**
     * Creates and returns a new instance of [FaceMatchingService].
     *
     * @return a fully initialized instance of DocumentFieldDetectorService
     *         ready to perform document field detection operations.
     */
    fun createDetectorService(): FaceMatchingService {
        return FaceMatchingService.create()
    }
}