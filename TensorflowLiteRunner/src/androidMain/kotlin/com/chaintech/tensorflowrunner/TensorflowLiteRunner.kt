@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.chaintech.tensorflowrunner

import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

actual class TensorflowLiteRunner {
    private var interpreter: Interpreter? = null

    private var inputShape: IntArray? = null
    private var inputBufferSize: Int = 0

    private var outputShapes: List<IntArray>? = null
    private var outputDataTypes: List<TensorDataType>? = null
    private var outputBuffers: List<ByteBuffer>? = null
    private var outputCount: Int = 0

    private var outputNames: List<String>? = null
    private var outputNameToIndexMap: Map<String, Int>? = null

    @Volatile
    private var initialized = false

    actual fun initialize(modelBytes: ByteArray?) {
        synchronized(this) {
            if (initialized) {
                println("TFLiteRunner: Already initialized")
                return
            }

            try {
                if (modelBytes == null) {
                    initialized = false
                    println("TFLiteRunner: modelBytes == null")
                    return
                }

                // Create model buffer
                val modelBuffer = ByteBuffer.allocateDirect(modelBytes.size).apply {
                    order(ByteOrder.nativeOrder())
                    put(modelBytes)
                    rewind()
                }

                // Configure interpreter options
                val options = Interpreter.Options().apply {
                    // Set number of threads for CPU execution
                    setNumThreads(4)

                    try {
                        // NNAPI for hardware acceleration
                        setUseNNAPI(true)
                        println("TFLiteRunner: Using NNAPI")
                    } catch (e: Exception) {
                        println("TFLiteRunner: NNAPI not available: ${e.message}")
                    }

                    // Enable XNNPack for optimized CPU operations
                    try {
                        setUseXNNPACK(true)
                        println("TFLiteRunner: Using XNNPack")
                    } catch (e: Exception) {
                        println("TFLiteRunner: XNNPack not available: ${e.message}")
                    }
                }

                interpreter = Interpreter(modelBuffer, options)

                interpreter?.let { interp ->
                    // Validate single input
                    val inputCount = interp.inputTensorCount
                    if (inputCount != 1) {
                        throw IllegalStateException("Model must have single input. Found: $inputCount inputs")
                    }

                    // Cache INPUT information
                    val inputTensor = interp.getInputTensor(0)
                    inputShape = inputTensor.shape()

                    val inputSize = inputShape!!.fold(1) { acc, dim -> acc * dim }
                    val inputBytesPerElement = getBytesPerElement(inputTensor.dataType())
                    inputBufferSize = inputSize * inputBytesPerElement

                    // Cache OUTPUT information (single or multiple)
                    outputCount = interp.outputTensorCount

                    outputShapes = List(outputCount) { index ->
                        interp.getOutputTensor(index).shape()
                    }

                    outputDataTypes = List(outputCount) { index ->
                        mapDataType(interp.getOutputTensor(index).dataType())
                    }

                    // Store output tensor names
                    outputNames = List(outputCount) { index ->
                        interp.getOutputTensor(index).name() ?: "output_$index"
                    }

                    // Create name to index mapping
                    outputNameToIndexMap = outputNames!!.withIndex().associate { (index, name) ->
                        name to index
                    }

                    // Pre-allocate output buffers
                    outputBuffers = List(outputCount) { index ->
                        val tensor = interp.getOutputTensor(index)
                        val shape = outputShapes!![index]
                        val size = shape.fold(1) { acc, dim -> acc * dim }
                        val bytesPerElement = getBytesPerElement(tensor.dataType())

                        ByteBuffer.allocateDirect(size * bytesPerElement)
                            .order(ByteOrder.nativeOrder())
                    }

                    // Logging
                    println("TFLiteRunner: Initialized successfully")
                    println("TFLiteRunner: Input shape: ${inputShape?.contentToString()}, size: $inputBufferSize bytes")
                    println("TFLiteRunner: Output count: $outputCount")
                    outputShapes?.forEachIndexed { index, shape ->
                        println("TFLiteRunner: Output[$index] shape: ${shape.contentToString()}, type: ${outputDataTypes!![index]}")
                        println("TFLiteRunner: Output[$index] name: '${outputNames!![index]}', shape: ${shape.contentToString()}, type: ${outputDataTypes!![index]}")
                    }
                }

                initialized = true

            } catch (e: Exception) {
                cleanup()
                throw IllegalStateException("Failed to initialize TFLite model: ${e.message}", e)
            }
        }
    }

    private fun getBytesPerElement(dataType: DataType): Int {
        return when (dataType) {
            DataType.FLOAT32 -> 4
            DataType.UINT8 -> 1
            DataType.INT8 -> 1
            DataType.INT32 -> 4
            DataType.INT64 -> 8
            else -> 4
        }
    }

    private fun mapDataType(dataType: DataType): TensorDataType {
        return when (dataType) {
            DataType.FLOAT32 -> TensorDataType.FLOAT32
            DataType.UINT8 -> TensorDataType.UINT8
            DataType.INT8 -> TensorDataType.INT8
            DataType.INT32 -> TensorDataType.INT32
            DataType.INT64 -> TensorDataType.INT64
            else -> TensorDataType.FLOAT32
        }
    }

    actual fun isInitialized(): Boolean = initialized

    actual fun runInference(inputData: ByteArray): InferenceResult {
        if (!initialized || interpreter == null) {
            throw IllegalStateException("TFLiteRunner not initialized. Call initialize() first.")
        }

        synchronized(this) {
            try {
                // Validate input size
                if (inputData.size != inputBufferSize) {
                    throw IllegalArgumentException(
                        "Input size mismatch. Expected: $inputBufferSize bytes, Got: ${inputData.size} bytes"
                    )
                }

                // Prepare input
                val inputBuffer = ByteBuffer.wrap(inputData).order(ByteOrder.nativeOrder())

                // Run inference
                if (outputCount == 1) {
                    // Single output - optimized path
                    outputBuffers!![0].rewind()
                    interpreter?.run(inputBuffer, outputBuffers!![0])
                } else {
                    // Multiple outputs
                    val outputMap = outputBuffers!!.mapIndexed { index, buffer ->
                        buffer.rewind()
                        index to buffer
                    }.toMap()

                    interpreter?.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputMap)
                }

                // Extract all outputs as ByteArray
                val outputs = outputBuffers!!.map { buffer ->
                    buffer.rewind()
                    ByteArray(buffer.remaining()).also { buffer.get(it) }
                }

                return InferenceResult(
                    outputs = outputs,
                    shapes = outputShapes!!,
                    dataTypes = outputDataTypes!!
                )

            } catch (e: Exception) {
                throw RuntimeException("Inference failed: ${e.message}", e)
            }
        }
    }

    /**
     * Get output tensor data by name after running inference
     * @param name The name of the output tensor
     * @return TensorData containing the output data, or null if not found
     */
//    actual fun getOutputByName1(name: String): TensorData? {
//        if (!initialized) {
//            throw IllegalStateException("TFLiteRunner not initialized. Call initialize() first.")
//        }
//
//        synchronized(this) {
//            // Find the index for the given output name
//            val outputIndex = outputNameToIndexMap?.get(name) ?: return null
//
//            // Get the corresponding buffer
//            val buffer = outputBuffers?.get(outputIndex) ?: return null
//
//            // Get tensor for quantization parameters
//            val tensor = interpreter?.getOutputTensor(outputIndex) ?: return null
//
//            // Extract data from buffer
//            buffer.rewind()
//            val data = ByteArray(buffer.remaining())
//            buffer.get(data)
//            buffer.rewind() // Reset for next use
//
//            // Get quantization parameters if available
//            val quantizationParams = try {
//                tensor.quantizationParams()
//            } catch (e: Exception) {
//                null
//            }
//
//            return TensorData(
//                data = data,
//                scale = quantizationParams?.scale ?: 1.0f,
//                zeroPoint = quantizationParams?.zeroPoint ?: 0
//            )
//        }
//    }

    actual fun getOutputByName(name: String): TensorData? {
        if (!initialized) {
            throw IllegalStateException("TFLiteRunner not initialized. Call initialize() first.")
        }

        synchronized(this) {
            for (i in 0 until getOutputCount()) {
                val tensor = interpreter?.getOutputTensor(i) ?: return null
                if (tensor.name() == name) {
                    // Get the corresponding buffer
                    val outputBuffer = outputBuffers?.get(i) ?: return null

                    // Extract data from buffer
                    outputBuffer.rewind()
                    val data = ByteArray(outputBuffer.remaining())
                    outputBuffer.get(data)
                    outputBuffer.rewind() // Reset for next use

                    val params = tensor.quantizationParams()
                    return TensorData(
                        data = data,
                        scale = params?.scale ?: 1.0f,
                        zeroPoint = params?.zeroPoint ?: 0
                    )
                }
            }
        }

        return null
    }

    /**
     * Get all output tensor names
     * @return List of output tensor names
     */
    actual fun getOutputNames(): List<String> {
        return outputNames?.toList() ?: throw IllegalStateException("Not initialized")
    }

    actual fun getInputShape(): IntArray {
        return inputShape?.copyOf() ?: throw IllegalStateException("Not initialized")
    }

    actual fun getOutputShapes(): List<IntArray> {
        return outputShapes?.map { it.copyOf() } ?: throw IllegalStateException("Not initialized")
    }

    actual fun getOutputCount(): Int = outputCount

    actual fun dispose() {
        synchronized(this) {
            cleanup()
        }
    }

    private fun cleanup() {
        try {
            interpreter?.close()
        } catch (e: Exception) {
            println("TFLiteRunner: Error during cleanup: ${e.message}")
        } finally {
            interpreter = null
            inputShape = null
            inputBufferSize = 0
            outputShapes = null
            outputDataTypes = null
            outputBuffers = null
            outputNames = null
            outputNameToIndexMap = null
            outputCount = 0
            initialized = false
            println("TFLiteRunner: Cleaned up")
        }
    }
}