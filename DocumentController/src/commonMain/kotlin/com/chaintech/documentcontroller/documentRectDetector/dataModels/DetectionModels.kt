package com.chaintech.documentcontroller.documentRectDetector.dataModels

import androidx.compose.ui.graphics.ImageBitmap
import com.chaintech.documentcontroller.docdetector.dataModels.BoundingBox

/**
 * Internal data class for raw detection output from model
 */
internal data class DetectorBoundingBox(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val centerX: Float,
    val centerY: Float,
    val width: Float,
    val height: Float,
    val confidenceScore: Float,
    val classId: Int,
    val className: String
)

/**
 * Public data class exposed to host app
 */
data class DocumentRegionRect(
    val className: DocumentRectClassName,
    val classId: Int,
    val boundingBox: BoundingBox,
    val confidence: Float,
    var image: ImageBitmap
)



/**
 * Size representation for cross-platform use
 */
internal data class SizeF(
    val width: Float,
    val height: Float
)

sealed class DocumentValidationResult {
    object ValidWithTesseract : DocumentValidationResult()
    object ValidWithOCR : DocumentValidationResult()
    object Failed : DocumentValidationResult()
}

/**
 * Document field types detected by the model
 */
enum class DocumentRectClassName(val value: String) {
    ADDRESS("address"),
    CITIZENSHIP("citizenship"),
    DATE_OF_BIRTH("date_of_birth"),
    DOCUMENT_EXPIRY_DATE("document_expiry_date"),
    DOCUMENT_ISSUE_DATE("document_issue_date"),
    EMAIL("email"),
    FULL_NAME("full_name"),
    GIVEN_NAME("given_name"),
    ID_NUMBER("id_number"),
    ISSUING_COUNTRY("issuing_country"),
    MRZ("mrz"),
    NATIONALITY("nationality"),
    NFC_DOCUMENT("nfc_document"),
    PHONE_NUMBER("phone_number"),
    PHOTO("photo"),
    QR_CODE("qr_code"),
    SEX("sex"),
    SIGNATURE("signature"),
    SURNAME("surname"),
    NONE("None");

    companion object {
        fun fromString(value: String): DocumentRectClassName {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: NONE
        }
    }
}