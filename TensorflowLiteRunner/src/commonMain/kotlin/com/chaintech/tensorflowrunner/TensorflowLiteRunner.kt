@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.chaintech.tensorflowrunner

/**
 * Result of model inference
 * - Single input → Single/Multiple outputs
 */
data class InferenceResult(
    val outputs: List<ByteArray>,      // Raw output data
    val shapes: List<IntArray>,         // Shape for each output
    val dataTypes: List<TensorDataType> // Data type for each output
) {
    // Convenience for single output models
    val output: ByteArray
        get() {
            if (outputs.size != 1) {
                throw IllegalStateException("Model has ${outputs.size} outputs. Use outputs[index] instead.")
            }
            return outputs[0]
        }

    val shape: IntArray
        get() {
            if (shapes.size != 1) {
                throw IllegalStateException("Model has ${shapes.size} outputs. Use shapes[index] instead.")
            }
            return shapes[0]
        }
}

enum class TensorDataType {
    FLOAT32,  // 4 bytes per element
    UINT8,    // 1 byte per element
    INT8,     // 1 byte per element
    INT32,    // 4 bytes per element
    INT64     // 8 bytes per element
}

expect class TensorflowLiteRunner() {
    fun initialize(modelBytes: ByteArray?)
    fun isInitialized(): Boolean

    // Single input, returns structured result with all outputs
    fun runInference(inputData: ByteArray): InferenceResult

    // Metadata
    fun getInputShape(): IntArray
    fun getOutputShapes(): List<IntArray>
    fun getOutputCount(): Int

    fun getOutputByName(name: String): TensorData?
    fun getOutputNames(): List<String>

    fun dispose()
}

data class TensorData(
    val data: ByteArray,
    val scale: Float = 1.0f,
    val zeroPoint: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as TensorData

        if (!data.contentEquals(other.data)) return false
        if (scale != other.scale) return false
        if (zeroPoint != other.zeroPoint) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + scale.hashCode()
        result = 31 * result + zeroPoint
        return result
    }
}