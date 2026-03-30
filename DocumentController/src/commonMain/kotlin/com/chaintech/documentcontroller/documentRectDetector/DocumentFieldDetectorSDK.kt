package com.chaintech.documentcontroller.documentRectDetector

import com.chaintech.documentcontroller.documentRectDetector.detectorService.DocumentFieldDetectorService


/**
 * Singleton object that provides a factory method for creating instances
 * of the DocumentFieldDetectorService.
 *
 * This acts as the main entry point to initialize and access the
 * document field detection functionality within the SDK.
 */
object DocumentFieldDetectorSDK {
    /**
     * Creates and returns a new instance of [DocumentFieldDetectorService].
     *
     * @return a fully initialized instance of DocumentFieldDetectorService
     *         ready to perform document field detection operations.
     */
    fun createDetectorService(): DocumentFieldDetectorService {
        return DocumentFieldDetectorService.create()
    }
}