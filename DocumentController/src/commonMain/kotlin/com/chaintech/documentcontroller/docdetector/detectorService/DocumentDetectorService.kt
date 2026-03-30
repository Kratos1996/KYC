package com.chaintech.documentcontroller.docdetector.detectorService

import androidx.compose.ui.graphics.ImageBitmap
import com.chaintech.documentcontroller.docdetector.dataModels.DetectionRegionRect

interface DocumentDetectorService {

    suspend fun initialize()

    suspend fun detectDocument(
        image: ImageBitmap,
        imageWidth: Int,
        imageHeight: Int
    ): DocumentDetectorData

    fun dispose()

    companion object {
        fun create(): DocumentDetectorService {
            return DocumentDetectorImpl()
        }
    }
}


