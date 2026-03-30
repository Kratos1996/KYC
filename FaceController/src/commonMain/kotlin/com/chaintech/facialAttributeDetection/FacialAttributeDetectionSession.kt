@file:OptIn(ExperimentalTime::class)

package com.chaintech.facialAttributeDetection

import androidx.compose.ui.graphics.ImageBitmap
import com.chaintech.facecropper.FaceCropper
import com.chaintech.facialAttributeDetection.dataModels.FacialAttributeError
import com.chaintech.facialAttributeDetection.dataModels.FacialAttributesResult
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Face mask detection session that manages detection over multiple frames
 */
class FacialAttributeDetectionSession(
    private val facialAttributeDetectorService: FacialAttributeDetectionImpl = FacialAttributeDetectionImpl(),
    val faceCropper: FaceCropper,
    private val maxFramesToCheck: Int = 5,
    private val confidenceThreshold: Int = 5
) {
    private var frameCount = 0
    private val results = mutableListOf<Boolean>()
    private var isComplete = false
    private var finalResult: Boolean? = null
    private var facialAttributesResult: FacialAttributesResult? = null

    // Time-based cooldown properties
    private var lastCompletionTime: Instant? = null
    private var isInCooldownPeriod = false
    private val noMaskCooldownDuration: Duration = 10.seconds  // 10 seconds for no mask
    private val maskCooldownDuration: Duration = 500.milliseconds  // 0.5 seconds for mask detected

    suspend fun initialize() {
        facialAttributeDetectorService.initialize()
    }

    fun resetAttributeDetectionSession() {
        frameCount = 0
        results.clear()
        isComplete = false
        finalResult = null
        exitCooldownAndReset()
    }

    fun dispose() {
        resetAttributeDetectionSession()
        faceCropper.close()
        facialAttributeDetectorService.dispose()
    }

    // MARK: - Public State Properties
    val currentState: DetectionState
        get() = when {
            isInCooldownPeriod -> DetectionState.Cooldown(remainingCooldownTime)
            isComplete -> DetectionState.Completed(finalResult)
            else -> DetectionState.Detecting(frameCount.toFloat() / maxFramesToCheck.toFloat())
        }

    val remainingCooldownTime: Duration
        get() {
            val lastTime = lastCompletionTime ?: return Duration.ZERO

            val elapsed = Clock.System.now() - lastTime
            val totalCooldown =
                if (finalResult == true) maskCooldownDuration else noMaskCooldownDuration
            return (totalCooldown - elapsed).coerceAtLeast(Duration.ZERO)
        }

    // MARK: Facial Occlusion Detection Method
    suspend fun detectFacialOcclusion(
        image: ImageBitmap,
    ): FacialAttributesResult? {
        // Check if we're in cooldown period
        if (isInCooldownPeriod) {
            lastCompletionTime?.let { lastTime ->
                if (shouldExitCooldown(lastTime, finalResult)) {
                    exitCooldownAndReset()
                } else {
                    // Still in cooldown, return cached result
                    return facialAttributesResult
                }
            }
        }

        // Return cached result if complete (but not in cooldown)
        if (isComplete) {
            return facialAttributesResult
        }

        // Check frame limit
        if (frameCount >= maxFramesToCheck) {
            completeDetection()
            enterCooldownPeriod()
            return facialAttributesResult
        }

        // Get face image with padding
        faceCropper.getCroppedFaceUsingBitmap(image = image, expandRatio = 1.2f)
            .fold(
                onSuccess = { result ->
                    return try {
                        facialAttributesResult = null
                        val facialAttributesResults =
                            facialAttributeDetectorService.getFacialAttributes(result.bitmap)

                        facialAttributesResult = facialAttributesResults

                        val hasFacialOcclusion = facialAttributesResults.mask.prediction ||
                                facialAttributesResults.sunglasses.prediction

                        frameCount++
                        results.add(hasFacialOcclusion)

                        // Check for early completion based on consecutive results
                        if (shouldCompleteEarly()) {
                            completeDetection()
                            enterCooldownPeriod()
                            return facialAttributesResult
                        }

                        // Return current prediction if not complete
                        facialAttributesResult
                    } catch (e: FacialAttributeError) {
                        println("FacialAttributeDetectionSession: FacialAttributeError: ${e.message}")
                        facialAttributesResult // Return last known result instead of null
                    } catch (e: Exception) {
                        println("FacialAttributeDetectionSession: Exception: ${e.message}")
                        facialAttributesResult // Return last known result instead of null
                    }
                },
                onFailure = { exception ->
                    println("FacialAttributeDetectionSession: ${exception.message}")
                    faceCropper.close()
                    return null
                }
            )
    }

    // MARK: - Cooldown Management
    private fun shouldExitCooldown(lastCompletionTime: Instant, lastResult: Boolean?): Boolean {
        val currentTime = kotlin.time.Clock.System.now()
        val timeSinceCompletion = currentTime - lastCompletionTime

        lastResult ?: return true

        return if (lastResult) {
            // Mask was detected - check again after 0.5 seconds
            timeSinceCompletion >= maskCooldownDuration
        } else {
            // No mask detected - wait 10 seconds before checking again
            timeSinceCompletion >= noMaskCooldownDuration
        }
    }

    private fun enterCooldownPeriod() {
        isInCooldownPeriod = true
        lastCompletionTime = kotlin.time.Clock.System.now()

        val cooldownDuration =
            if (finalResult == true) maskCooldownDuration else noMaskCooldownDuration
        println("Entering cooldown period for ${cooldownDuration.inWholeSeconds} seconds. Last result: ${if (finalResult == true) "Mask" else "No Mask"}")
    }

    private fun exitCooldownAndReset() {
        isInCooldownPeriod = false
        lastCompletionTime = null
        frameCount = 0
        results.clear()
        isComplete = false
    }

    private fun shouldCompleteEarly(): Boolean {
        // Need at least the confidence threshold number of results
        if (results.size < confidenceThreshold) return false

        // Check if the last 'confidenceThreshold' results are all the same
        val startIndex = results.size - confidenceThreshold
        val recentResults = results.subList(startIndex, results.size)
        val firstResult = recentResults.first()

        return recentResults.all { it == firstResult }
    }

    private fun completeDetection() {
        isComplete = true
        val maskCount = results.count { it }
        finalResult = maskCount > results.size / 2

        println("Detection completed. Frames processed: $frameCount, Final result: ${if (finalResult == true) "Mask" else "No Mask"}")
    }
}

/**
 * Detection state enum representing the current state of detection
 */
sealed class DetectionState {
    data class Detecting(val progress: Float) : DetectionState()
    data class Completed(val result: Boolean?) : DetectionState()
    data class Cooldown(val timeRemaining: Duration) : DetectionState()

    val description: String
        get() = when (this) {
            is Detecting -> "Detecting... ${(progress * 100).toInt()}%"
            is Completed -> if (result == true) "Mask Detected" else "No Mask"
            is Cooldown -> "Waiting ${timeRemaining.inWholeSeconds}s"
        }
}