package com.chaintech.mrzparsersdk.tesseractOcr

import androidx.compose.ui.graphics.ImageBitmap

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class TesseractManager {
    suspend fun initialize(language: String): Boolean
    suspend fun extractTextFromBitmap(imageBitmap: ImageBitmap): String?
    suspend fun setPageSegmentationMode(mode: PageSegmentationMode)
    suspend fun release()
}

enum class PageSegmentationMode {
    AUTO,
    SINGLE_BLOCK,
    SINGLE_LINE,
    SINGLE_WORD,
    SINGLE_CHAR
}

expect fun createTesseractManager(): TesseractManager
