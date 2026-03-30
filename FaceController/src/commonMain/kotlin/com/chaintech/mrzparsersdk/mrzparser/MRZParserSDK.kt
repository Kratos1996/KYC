package com.chaintech.mrzparsersdk.mrzparser

import com.chaintech.mrzparsersdk.mrzparser.service.MRZExtractionService
import com.chaintech.mrzparsersdk.mrzparser.service.MRZExtractionServiceImpl

/**
 * Entry point for the MRZ Parser SDK.
 *
 * This object provides factory methods to create instances of
 * [com.chaintech.mrzparsersdk.service.MRZExtractionService], which can be used to process MRZ (Machine Readable Zone)
 * strings from passports, IDs, and travel documents.
 *
 * ---
 * ## Usage
 *
 * ### 🔹 Android (Kotlin Example)
 * ```kotlin
 * val service = MRZParserSDK.createMRZExtractionService()
 *
 * // Suspend function with exception handling
 * try {
 *     val result = service.processMRZ("MRZ_STRING_HERE")
 *     Log.d("MRZParser", "Result: $result")
 * } catch (e: MRZError) {
 *     Log.e("MRZParser", "Failed: ${e.message}")
 * }
 *
 * // Or safe version (Result<T>)
 * val resultSafe = service.processMRZSafe("MRZ_STRING_HERE")
 * resultSafe.onSuccess {
 *     Log.d("MRZParser", "Parsed: $it")
 * }.onFailure {
 *     Log.e("MRZParser", "Error: ${it.message}")
 * }
 * ```
 *
 * ### 🔹 Android (Java Example)
 * ```java
 * MRZExtractionService service = MRZParserSDK.INSTANCE.createMRZExtractionService();
 *
 * // Suspend functions must be called via coroutines (Kotlin interop)
 * // OR wrap in a coroutineScope using lifecycleScope in Android.
 * ```
 *
 * ---
 * ### 🔹 iOS (Swift Example)
 * ```swift
 * let service = MRZParserSDK().createMRZExtractionService()
 *
 * // Using async/await with try/catch
 * do {
 *     let result = try await service.processMRZ(recognizedString: mrzString)
 *     print("Parsed result: \(result)")
 * } catch {
 *     print("Error: \(error.localizedDescription)")
 * }
 *
 * // Using Result-based safe method
 * let resultSafe = await service.processMRZSafe(recognizedString: mrzString)
 * switch resultSafe {
 * case .success(let data):
 *     print("Parsed result: \(data)")
 * case .failure(let err):
 *     print("Error: \(err.localizedDescription)")
 * }
 * ```
 *
 * ---
 * @return a fresh instance of [com.chaintech.mrzparsersdk.service.MRZExtractionService].
 */
object MRZParserSDK {

    /**
     * Creates and returns an instance of [com.chaintech.mrzparsersdk.service.MRZExtractionService].
     *
     * - Provides two approaches for MRZ processing:
     *   1. `processMRZ(recognizedString: String)`
     *      → Throws [MRZError] (bridged to NSError in Swift).
     *      → Best for try/catch style error handling.
     *
     *   2. `processMRZSafe(recognizedString: String)`
     *      → Never throws.
     *      → Returns a [Result] (success or failure).
     *      → Best for functional style error handling.
     */
    fun createMRZExtractionService(): MRZExtractionService {
        return MRZExtractionServiceImpl()
    }
}