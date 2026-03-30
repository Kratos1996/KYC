package com.chaintech.facialAttributeDetection.dataModels

data class FacialAttributesResult(
    val idFeature: FloatArray,                    // 512-dim normalized embedding
    val livenessFeature: FloatArray,              // 32-dim embedding
    val eyeCloseness: PredictionResult<String>,   // [open, closed]
    val glasses: PredictionResult<Boolean>,       // [no-glasses, glasses]
    val mask: PredictionResult<Boolean>,          // [no-mask, mask]
    val sunglasses: PredictionResult<Boolean>     // [no-sunglasses, sunglasses]
) {
    /**
     * Nested data class to represent prediction results with confidence
     */
    data class PredictionResult<T>(
        val confidence: Float,
        val prediction: T
    )

    // Override equals and hashCode since we're using FloatArray
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as FacialAttributesResult

        if (!idFeature.contentEquals(other.idFeature)) return false
        if (!livenessFeature.contentEquals(other.livenessFeature)) return false
        if (eyeCloseness != other.eyeCloseness) return false
        if (glasses != other.glasses) return false
        if (mask != other.mask) return false
        if (sunglasses != other.sunglasses) return false

        return true
    }

    override fun hashCode(): Int {
        var result = idFeature.contentHashCode()
        result = 31 * result + livenessFeature.contentHashCode()
        result = 31 * result + eyeCloseness.hashCode()
        result = 31 * result + glasses.hashCode()
        result = 31 * result + mask.hashCode()
        result = 31 * result + sunglasses.hashCode()
        return result
    }
}

sealed class FacialAttributeError(message: String) : Exception() {
    class InterpreterNotInitialized(message: String = "Interpreter not initialized") : FacialAttributeError(message)
    class InvalidInput(message: String = "Invalid input image") : FacialAttributeError(message)
    class InferenceFailed(message: String = "Inference failed") : FacialAttributeError(message)
    class OutputNotFound(message: String) : FacialAttributeError(message)
}
