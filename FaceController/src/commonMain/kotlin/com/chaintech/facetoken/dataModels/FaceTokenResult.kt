package com.chaintech.facetoken.dataModels

sealed class FaceTokenException(message: String) : Exception(message) {
    class ImagePreprocessingError(message: String) : FaceTokenException(message)
    class ModelNotInitialized(
        message: String = "Model interpreter not initialized"
    ) : FaceTokenException(message)
    class EmbeddingGenerationError(message: String) : FaceTokenException(message)
    class InvalidInput(message: String) : FaceTokenException(message)
}
