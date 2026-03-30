package com.chaintech.mrzparsersdk.tesseractOcr

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.ByteArrayOutputStream
import java.io.File

@SuppressLint("StaticFieldLeak")
object AndroidAppContext {
    lateinit var applicationContext: Context
}

actual fun createTesseractManager(): TesseractManager {
    return TesseractManager(AndroidAppContext.applicationContext)
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class TesseractManager(private val context: Context) {
    private var tessBaseAPI: TessBaseAPI? = null
    private val tessDataPath: String = File(context.filesDir, "tessdata").absolutePath

    actual suspend fun initialize(language: String): Boolean {
        copyTessDataFiles()

        tessBaseAPI = TessBaseAPI()

        val tessDataPath = File(tessDataPath).parent
        return try {
            tessBaseAPI?.init(tessDataPath, language) ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun copyTessDataFiles() {
        val tessDataDir = File(context.filesDir, "tessdata")
        if (!tessDataDir.exists()) tessDataDir.mkdirs()

        val trainedDataFile = File(tessDataDir, "mrz.traineddata")
        if (!trainedDataFile.exists()) {
            context.assets.open("tessdata/mrz.traineddata").use { input ->
                trainedDataFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    actual suspend fun extractTextFromBitmap(imageBitmap: ImageBitmap): String? {
        val imageBytes = imageBitmapToByteArray(imageBitmap)
        val bitmap = BitmapFactory.decodeByteArray(
            imageBytes,
            0,
            imageBytes.size
        ) ?: return null

        return try {
            tessBaseAPI?.setImage(bitmap)
            tessBaseAPI?.utF8Text
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            bitmap.recycle()
        }
    }

    actual suspend fun setPageSegmentationMode(mode: PageSegmentationMode) {
        val tessMode = when (mode) {
            PageSegmentationMode.AUTO -> TessBaseAPI.PageSegMode.PSM_AUTO
            PageSegmentationMode.SINGLE_BLOCK -> TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK
            PageSegmentationMode.SINGLE_LINE -> TessBaseAPI.PageSegMode.PSM_SINGLE_LINE
            PageSegmentationMode.SINGLE_WORD -> TessBaseAPI.PageSegMode.PSM_SINGLE_WORD
            PageSegmentationMode.SINGLE_CHAR -> TessBaseAPI.PageSegMode.PSM_SINGLE_CHAR
        }
        tessBaseAPI?.pageSegMode = tessMode
    }

    actual suspend fun release() {
        tessBaseAPI?.recycle()
        tessBaseAPI = null
    }

    private fun imageBitmapToByteArray(image: ImageBitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}