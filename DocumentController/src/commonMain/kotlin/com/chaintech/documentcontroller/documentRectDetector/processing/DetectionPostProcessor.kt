package com.chaintech.documentcontroller.documentRectDetector.processing

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.chaintech.documentcontroller.docdetector.dataModels.BoundingBox
import com.chaintech.documentcontroller.documentRectDetector.dataModels.DetectorBoundingBox
import com.chaintech.documentcontroller.documentRectDetector.dataModels.DocumentRectClassName
import com.chaintech.documentcontroller.documentRectDetector.dataModels.DocumentRegionRect
import com.chaintech.documentcontroller.documentRectDetector.dataModels.SizeF
import com.chaintech.documentcontroller.documentRectDetector.utils.DetectionCalculations

/**
 * Internal processor for detection logic - not exposed to host app
 */
internal class DetectionPostProcessor() {
    private val confidenceThreshold: Float = 0.3f
    private val iouThreshold: Float = 0.5f
    private val classLabels: List<String> = listOf(
        "address", "citizenship", "date_of_birth", "document_expiry_date", "document_issue_date", "email",
        "full_name", "given_name", "id_number", "issuing_country", "mrz", "nationality", "nfc_document",
        "phone_number", "photo", "qr_code", "sex", "signature", "surname"
    )

    private var numChannel: Int = 0
    private var numElements: Int = 0

    fun initialize(numChannel: Int, numElements: Int) {
        this@DetectionPostProcessor.numChannel = numChannel
        this@DetectionPostProcessor.numElements = numElements
    }

    /**
     * Process raw model output into bounding boxes
     */
    fun processOutput(outputData: FloatArray): List<DetectorBoundingBox> {
        val boxes = mutableListOf<DetectorBoundingBox>()

        for (c in 0 until numElements) {
            // Extract confidence scores for each class (skipping first 4 values which are coordinates)
            val confidenceScores = (4 until numChannel).map {
                outputData[c + numElements * it]
            }

            val maxConfidence = confidenceScores.maxOrNull() ?: 0f
            val maxIndex = confidenceScores.indexOf(maxConfidence)

            if (maxConfidence <= confidenceThreshold || maxIndex >= classLabels.size) {
                continue
            }

            // Extract coordinates
            val cx = outputData[c]
            val cy = outputData[c + numElements]
            val w = outputData[c + numElements * 2]
            val h = outputData[c + numElements * 3]

            // Convert center coordinates to corner coordinates
            val x1 = cx - w / 2
            val y1 = cy - h / 2
            val x2 = cx + w / 2
            val y2 = cy + h / 2

            // Skip invalid boxes
            if (x1 < 0 || y1 < 0 || x2 > 1 || y2 > 1) {
                continue
            }

            boxes.add(
                DetectorBoundingBox(
                    x1 = x1,
                    y1 = y1,
                    x2 = x2,
                    y2 = y2,
                    centerX = cx,
                    centerY = cy,
                    width = w,
                    height = h,
                    confidenceScore = maxConfidence,
                    classId = maxIndex,
                    className = classLabels[maxIndex]
                )
            )
        }

        return applyNMS(boxes)
    }

    /**
     * Apply Non-Maximum Suppression to filter overlapping boxes
     */
    private fun applyNMS(boxes: List<DetectorBoundingBox>): List<DetectorBoundingBox> {
        if (boxes.isEmpty()) return emptyList()

        val sortedBoxes = boxes.sortedByDescending { it.confidenceScore }.toMutableList()
        val selectedBoxes = mutableListOf<DetectorBoundingBox>()

        while (sortedBoxes.isNotEmpty()) {
            val selected = sortedBoxes.removeAt(0)
            selectedBoxes.add(selected)

            sortedBoxes.removeAll { box ->
                DetectionCalculations.calculateIoU(
                    box1 = selected,
                    box2 = box
                ) >= iouThreshold
            }
        }

        return selectedBoxes
    }

    /**
     * Prepare final output with scaled bounding boxes
     */
    fun prepareDetectionResults(
        image: ImageBitmap,
        boundingBoxes: List<DetectorBoundingBox>,
        processedImageSize: SizeF,
        originalImageSize: SizeF
    ): List<DocumentRegionRect> {
        return boundingBoxes.map { detection ->
            val imageRect = DetectionCalculations.normalizedToRect(
                box = detection,
                imageWidth = processedImageSize.width,
                imageHeight = processedImageSize.height
            )

            val scaledRect = DetectionCalculations.scaleRect(
                rect = imageRect,
                fromSize = processedImageSize,
                toSize = originalImageSize
            )

            val cropImage = cropImageBitmap(image, box = scaledRect)

            DocumentRegionRect(
                className = DocumentRectClassName.Companion.fromString(detection.className),
                classId = detection.classId,
                boundingBox = scaledRect,
                confidence = detection.confidenceScore,
                image = cropImage
            )
        }
    }

    private fun cropImageBitmap(source: ImageBitmap, box: BoundingBox): ImageBitmap {
        val left = box.x.toInt().coerceAtLeast(0)
        val top = box.y.toInt().coerceAtLeast(0)
        val width = box.width.toInt().coerceAtMost(source.width - left)
        val height = box.height.toInt().coerceAtMost(source.height - top)

        val cropped = ImageBitmap(width, height)
        val canvas = Canvas(cropped)
        val paint = Paint()

        canvas.drawImageRect(
            image = source,
            srcOffset = IntOffset(left, top),
            srcSize = IntSize(width, height),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(width, height),
            paint = paint
        )

        return cropped
    }
}