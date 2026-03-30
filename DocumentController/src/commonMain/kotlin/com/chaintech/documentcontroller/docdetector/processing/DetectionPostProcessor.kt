package com.chaintech.documentcontroller.docdetector.processing

import com.chaintech.documentcontroller.docdetector.dataModels.BoundingBox
import com.chaintech.documentcontroller.docdetector.dataModels.DetectionRegionRect

internal class DetectionPostProcessor() {
    private val confidenceThreshold: Float = 0.7f
    private val iouThreshold: Float = 0.5f

    private var numChannel: Int = 0
    private var numElements: Int = 0

    fun initialize(numChannel: Int, numElements: Int) {
        this@DetectionPostProcessor.numChannel = numChannel
        this@DetectionPostProcessor.numElements = numElements
    }

    /**
     * Process raw model output into bounding boxes
     */
    fun processOutput(outputData: FloatArray,imageWidth: Int, imageHeight: Int): List<DetectionRegionRect> {
        val numBoxes = numElements
        val sx = imageWidth.toFloat() / 640
        val sy = imageHeight.toFloat() / 640

        val detections = mutableListOf<DetectionRegionRect>()

        for (i in 0 until numBoxes) {
            val cx = outputData[0 * numBoxes + i] // centerX
            val cy = outputData[1 * numBoxes + i] // centerY
            val w  = outputData[2 * numBoxes + i]
            val h  = outputData[3 * numBoxes + i]
            val conf = outputData[4 * numBoxes + i]

            if (conf < confidenceThreshold) continue

            // Scale to image size
            val scaledCx = cx * sx
            val scaledCy = cy * sy
            val scaledW = w * sx
            val scaledH = h * sy

            // Convert center → top-left
            val left = (scaledCx - scaledW / 2f).coerceIn(0f, imageWidth.toFloat())
            val top = (scaledCy - scaledH / 2f).coerceIn(0f, imageHeight.toFloat())
            val right = (scaledCx + scaledW / 2f).coerceIn(0f, imageWidth.toFloat())
            val bottom = (scaledCy + scaledH / 2f).coerceIn(0f, imageHeight.toFloat())

            val box = BoundingBox(
                x = left,
                y = top,
                width = right - left,
                height = bottom - top
            )

            detections.add(DetectionRegionRect(box, conf))
        }

        // Apply Non-Max Suppression (use your nms() function)
        return nms(detections)
    }

    fun nms(detections: List<DetectionRegionRect>): List<DetectionRegionRect> {
        if (detections.isEmpty()) return emptyList()

        val sorted = detections.sortedByDescending { it.confidence }
        val selected = mutableListOf<DetectionRegionRect>()

        for (det in sorted) {
            val keep = selected.none { iou(it.boundingBox, det.boundingBox) > iouThreshold }
            if (keep) selected.add(det)
        }

        return selected
    }

    fun iou(a: BoundingBox, b: BoundingBox): Float {
        val x1 = maxOf(a.x, b.x)
        val y1 = maxOf(a.y, b.y)
        val x2 = minOf(a.x + a.width, b.x + b.width)
        val y2 = minOf(a.y + a.height, b.y + b.height)

        val intersection = maxOf(0f, x2 - x1) * maxOf(0f, y2 - y1)
        val union = (a.width * a.height) + (b.width * b.height) - intersection

        return if (union <= 0f) 0f else intersection / union
    }
}