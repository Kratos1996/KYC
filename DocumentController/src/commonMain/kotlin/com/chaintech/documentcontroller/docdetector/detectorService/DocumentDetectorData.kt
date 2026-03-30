package com.chaintech.documentcontroller.docdetector.detectorService

import androidx.compose.ui.graphics.ImageBitmap
import com.chaintech.documentcontroller.docdetector.dataModels.DetectionRegionRect

data class DocumentDetectorData(
    val dectedListData : List<DetectionRegionRect>,
    val croppedBitmap: ImageBitmap
)
