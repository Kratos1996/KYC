package com.chaintech.documentcontroller.docdetector

import com.chaintech.documentcontroller.docdetector.detectorService.DocumentDetectorService

object DocumentDetectorSDK {
    fun createDetectorService(): DocumentDetectorService {
        return DocumentDetectorService.create()
    }
}