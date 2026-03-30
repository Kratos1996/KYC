package com.chaintech.documentcontroller.docdetector.internal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kyc.documentcontroller.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Loads TFLite model from resources
 */
internal object ModelLoader {
    private var cachedModel: ByteArray? = null
    private const val MODEL_PATH = "files/docDetector_ml.tflite"

    suspend fun loadModel(): ByteArray? = withContext(Dispatchers.Default) {
        cachedModel ?: tryLoadWithGeneratedAccessor().also {
            cachedModel = it
        }
    }

    fun clearCache() {
        cachedModel = null
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun tryLoadWithGeneratedAccessor(): ByteArray? {
        return try {
            val bytes = Res.readBytes(
                MODEL_PATH
            )
            bytes
        } catch (e: Exception) {
            println("documentDetector -> Generated accessor failed: ${e.message}")
            null
        }
    }
}