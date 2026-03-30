package com.chaintech.facematching.dataModels

/**
 * Result of face matching comparison
 */
data class FaceMatchingResult(
    val isSimilar: Boolean,
    val similarity: Float
)

data class FaceImageSize(
    val width: Int,
    val height: Int
)

data class FaceMatchInput(
    val firstImage: ByteArray,
    val secondImage: ByteArray,
    val firstFaceImageSize: FaceImageSize,
    val secondFaceImageSize: FaceImageSize,
    val threshold: Float = 0.5f
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as FaceMatchInput

        if (threshold != other.threshold) return false
        if (!firstImage.contentEquals(other.firstImage)) return false
        if (!secondImage.contentEquals(other.secondImage)) return false
        if (firstFaceImageSize != other.firstFaceImageSize) return false
        if (secondFaceImageSize != other.secondFaceImageSize) return false

        return true
    }

    override fun hashCode(): Int {
        var result = threshold.hashCode()
        result = 31 * result + firstImage.contentHashCode()
        result = 31 * result + secondImage.contentHashCode()
        result = 31 * result + firstFaceImageSize.hashCode()
        result = 31 * result + secondFaceImageSize.hashCode()
        return result
    }
}

sealed class FaceMatchingException(message: String) : Exception(message) {
    class ImagePreprocessingError(message: String) : FaceMatchingException(message)
    class ModelNotInitialized(
        message: String = "Model interpreter not initialized"
    ) : FaceMatchingException(message)
    class EmbeddingGenerationError(message: String) : FaceMatchingException(message)
    class InvalidInput(message: String) : FaceMatchingException(message)
}
