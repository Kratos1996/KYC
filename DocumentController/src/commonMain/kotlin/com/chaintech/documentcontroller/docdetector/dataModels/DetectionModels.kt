package com.chaintech.documentcontroller.docdetector.dataModels

/**
 * Internal data class for raw detection output from model
 */

data class BoundingBox(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

data class DetectionRegionRect(
    val boundingBox: BoundingBox,
    val confidence: Float,
    val classId: Int = 0,
    val className: String = "ID Card"
)

enum class DetectionRectClassName(val value: String) {
    IDCARD("id_card"),
    NONE("None");

    companion object {
        fun fromString(value: String): DetectionRectClassName {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: NONE
        }
    }
}