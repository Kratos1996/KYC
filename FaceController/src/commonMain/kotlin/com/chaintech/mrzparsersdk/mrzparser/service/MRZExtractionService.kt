package com.chaintech.mrzparsersdk.mrzparser.service

import androidx.compose.ui.graphics.ImageBitmap
import com.chaintech.mrzparsersdk.mrzparser.model.MRZException
import com.chaintech.mrzparsersdk.mrzparser.model.MRZResultModel
import kotlin.coroutines.cancellation.CancellationException

/**
 * This service provides MRZ extraction functionality.
 * It exposes two methods:
 * 1. `processMRZ` (with @Throws) → throws NSError in Swift, works with async/await try-catch
 * 2. `processMRZSafe` → returns Kotlin Result<T>, works cleanly with Swift Result handling
 */
interface MRZExtractionService {

    suspend fun initialize()

    @Throws(MRZException::class, CancellationException::class)
    suspend fun parseMrz(imageBitmap: ImageBitmap): MRZResultModel?

    fun dispose()

//    /**
//     * Suspend function that throws if parsing fails.
//     *
//     * @Throws will ensure this propagates as NSError in Swift.
//     */
//    @Throws(MRZException::class, CancellationException::class)
//    suspend fun processMRZ(imageBitmap: ImageBitmap): MRZResultModel
//
//    /**
//     * Safe version that never throws.
//     * Returns `Result.success` or `Result.failure`.
//     * Swift will see it as a `Result<MRZResultModel>` type.
//     */
//    suspend fun processMRZSafe(imageBitmap: ImageBitmap): Result<MRZResultModel>
}