package com.chaintech.faceantispoofing.dataModels

data class AntiSpoofingResult(
    val isSpoofed: Boolean,
    val antiSpoofingScore: String
)

sealed class FaceAntiSpoofingException(message: String) : Exception(message) {
    class ModelNotInitialized(
        message: String = "Model interpreter not initialized"
    ) : FaceAntiSpoofingException(message)
    class InvalidInput(message: String) : FaceAntiSpoofingException(message)
}

data class FaceAntiSpoofingInput(
    val image: ByteArray,
    val width: Int,
    val height: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as FaceAntiSpoofingInput

        if (width != other.width) return false
        if (height != other.height) return false
        if (!image.contentEquals(other.image)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + image.contentHashCode()
        return result
    }
}