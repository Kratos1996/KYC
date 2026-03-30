package com.chaintech.documentcontroller.documentRectDetector.utils

import com.chaintech.documentcontroller.docdetector.dataModels.BoundingBox
import com.chaintech.documentcontroller.documentRectDetector.dataModels.DetectorBoundingBox
import com.chaintech.documentcontroller.documentRectDetector.dataModels.SizeF

/**
 * Internal math utilities for detection calculations
 */
internal object DetectionCalculations {

    /**
     * Calculate Intersection over Union (IoU) between two bounding boxes
     */
    fun calculateIoU(box1: DetectorBoundingBox, box2: DetectorBoundingBox): Float {
        val x1 = maxOf(box1.x1, box2.x1)
        val y1 = maxOf(box1.y1, box2.y1)
        val x2 = minOf(box1.x2, box2.x2)
        val y2 = minOf(box1.y2, box2.y2)

        val intersectionWidth = maxOf(0f, x2 - x1)
        val intersectionHeight = maxOf(0f, y2 - y1)
        val intersection = intersectionWidth * intersectionHeight

        val area1 = box1.width * box1.height
        val area2 = box2.width * box2.height
        val union = area1 + area2 - intersection

        return if (union > 0) intersection / union else 0f
    }

    /**
     * Convert normalized bounding box to actual rectangle coordinates
     */
    fun normalizedToRect(
        box: DetectorBoundingBox,
        imageWidth: Float,
        imageHeight: Float
    ): BoundingBox {
        return BoundingBox(
            x = box.x1 * imageWidth,
            y = box.y1 * imageHeight,
            width = box.width * imageWidth,
            height = box.height * imageHeight
        )
    }

    /**
     * Scale a rectangle from one size to another
     */
    fun scaleRect(
        rect: BoundingBox,
        fromSize: SizeF,
        toSize: SizeF
    ): BoundingBox {
        val scaleX = toSize.width / fromSize.width
        val scaleY = toSize.height / fromSize.height

        return BoundingBox(
            x = rect.x * scaleX,
            y = rect.y * scaleY,
            width = rect.width * scaleX,
            height = rect.height * scaleY
        )
    }
}
