package com.chaintech.mrzparsersdk.mrzparser.service

import androidx.compose.ui.graphics.ImageBitmap
import com.chaintech.mrzparsersdk.mrzparser.extractionManager.MRZExtractionManager
import com.chaintech.mrzparsersdk.mrzparser.model.MRZException
import com.chaintech.mrzparsersdk.mrzparser.model.MRZResultModel
import com.chaintech.mrzparsersdk.tesseractOcr.PageSegmentationMode
import com.chaintech.mrzparsersdk.tesseractOcr.TesseractManager
import com.chaintech.mrzparsersdk.tesseractOcr.createTesseractManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

internal class MRZExtractionServiceImpl : MRZExtractionService {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mrzExtractionManager = MRZExtractionManager()
    private val tesseractManager: Deferred<TesseractManager> =
        scope.async(start = CoroutineStart.LAZY) {
            createTesseractManager()
        }

    override suspend fun initialize() {
        tesseractManager.start()
        if (tesseractManager.isActive || tesseractManager.isCompleted) {
            tesseractManager.await().initialize("mrz")
            tesseractManager.await().setPageSegmentationMode(PageSegmentationMode.AUTO)
        }
    }

    override suspend fun parseMrz(imageBitmap: ImageBitmap): MRZResultModel =
        withContext(Dispatchers.Default) {
            try {
                val tesseract = tesseractManager.await()
                val extractedText = tesseract.extractTextFromBitmap(imageBitmap)
                    ?: throw MRZException.Custom("No text could be extracted from the provided image.")

                mrzExtractionManager.performMRZExtraction(extractedText)
            } catch (e: Exception) {
                when (e) {
                    is MRZException -> throw e
                    is CancellationException -> throw e  // Don't wrap cancellation
                    else -> throw MRZException.Custom("Failed to parse MRZ: ${e.message}")
                }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun dispose() {
        if (tesseractManager.isCompleted && !tesseractManager.isCancelled) {
            try {
                runBlocking {
                    tesseractManager.getCompleted().release()
                }
            } catch (e: Exception) {
                // Log error but don't throw during disposal
                println("MRZExtractionServiceImpl dispose Error - ${e.message}")
            }
        }
        scope.cancel("MRZExtractionService is being disposed.")
    }
}