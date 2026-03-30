package com.chaintech.kyc.core

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.chaintech.documentcontroller.docdetector.dataModels.BoundingBox


/**
 * Utility object for Bitmap-related helper functions.
 */
object BitmapUtil {

    /**
     * Checks if the bitmap is valid (not null, not recycled, and has valid dimensions).
     */
    fun ImageBitmap?.isValid(): Boolean {
        return this != null  && this.width > 0 && this.height > 0
    }

    fun cropImageBitmap(source: ImageBitmap, box: BoundingBox): ImageBitmap {
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
