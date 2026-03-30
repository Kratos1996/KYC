package com.chaintech.googlemlkitocr

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.google.mlkit.vision.text.TextRecognizer as MLKitTextRecognizer

actual fun createTextRecognizer(): TextRecognizer {
    return AndroidTextRecognizer()
}

class AndroidTextRecognizer : TextRecognizer {

    private var recognizer: MLKitTextRecognizer? = null
    private val lock = Any()

    private fun getRecognizer(): MLKitTextRecognizer {
        synchronized(lock) {
            if (recognizer == null) {
                recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            }
            return recognizer!!
        }
    }

    override fun release() {
        synchronized(lock) {
            recognizer?.close()
            recognizer = null
        }
    }

    override suspend fun processImage(imageData: ImageBitmap): String = withContext(Dispatchers.IO) {
        try {
            val bitmap = imageData.asAndroidBitmap()
            require(!bitmap.isRecycled) { "Bitmap is recycled" }

            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val result = processText(inputImage)

            result.text
        } catch (e: Exception) {
            // If detector was closed, reset and retry once
            if (e.message?.contains("closed") == true) {
                retry(imageData)
            }
            throw OcrException.RecognitionException("Failed to process image: ${e.message}")
        }
    }

    private suspend fun processText(inputImage: InputImage): Text =
        suspendCancellableCoroutine { continuation ->
            getRecognizer().process(inputImage)
                .addOnSuccessListener { text ->
                    continuation.resume(text)
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }

    private suspend fun retry(imageData: ImageBitmap): String = withContext(Dispatchers.IO) {
        synchronized(lock) {
            recognizer = null
        }
        // Retry
        val bitmap = imageData.asAndroidBitmap()
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        val result = processText(inputImage)
        return@withContext result.text.ifEmpty {
            throw OcrException.RecognitionException("No text detected")
        }
    }
}