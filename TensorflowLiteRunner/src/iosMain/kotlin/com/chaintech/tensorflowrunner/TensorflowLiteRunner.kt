@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.chaintech.tensorflowrunner

import cocoapods.TFLTensorFlowLite.TFLInterpreter
import cocoapods.TFLTensorFlowLite.TFLInterpreterOptions
import cocoapods.TFLTensorFlowLite.TFLTensorDataType
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.Foundation.NSArray
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSNumber
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.dataWithBytes
import platform.Foundation.writeToFile
import platform.posix.memcpy
import kotlin.concurrent.Volatile

@OptIn(ExperimentalForeignApi::class)
actual class TensorflowLiteRunner {
    private var interpreter: TFLInterpreter? = null

    // Single input caching
    private var inputShape: IntArray? = null
    private var inputBufferSize: Int = 0

    // Multiple outputs support
    private var outputShapes: List<IntArray>? = null
    private var outputDataTypes: List<TensorDataType>? = null
    private var outputBufferSizes: List<Int>? = null
    private var outputCount: Int = 0

    private var outputNames: List<String>? = null
    private var outputNameToIndexMap: Map<String, Int>? = null

    @Volatile
    private var initialized = false

    @OptIn(BetaInteropApi::class)
    actual fun initialize(modelBytes: ByteArray?) {
        if (initialized) {
            println("TFLiteRunner iOS: Already initialized")
            return
        }

        try {
            if (modelBytes == null) {
                initialized = false
                println("TFLiteRunner iOS: modelBytes == null")
                return
            }

            // Convert ByteArray to NSData
            val nsData = modelBytes.usePinned { pinned ->
                NSData.dataWithBytes(pinned.addressOf(0), modelBytes.size.toULong())
            }

            // Save to temporary file
            val tempDir = NSTemporaryDirectory()
            val tempPath = tempDir + "temp_model_doc.tflite"

            if (!nsData.writeToFile(tempPath, atomically = true)) {
                throw IllegalStateException("Failed to write model to temp file")
            }

            // Create interpreter options
            val options = TFLInterpreterOptions()
            options.setNumberOfThreads(4u)
            options.setUseXNNPACK(true)

            // Create interpreter
            memScoped {
                val errorPtr = alloc<ObjCObjectVar<NSError?>>()

                println("TFLiteRunner iOS: Creating interpreter")

                interpreter = TFLInterpreter(
                    modelPath = tempPath,
                    options = options,
                    error = errorPtr.ptr
                )

                val error = errorPtr.value
                if (error != null) {
                    NSFileManager.defaultManager.removeItemAtPath(tempPath, null)
                    throw IllegalStateException("Failed to create interpreter: ${error.localizedDescription}")
                }

                println("TFLiteRunner iOS: Interpreter created successfully")
            }

            // Clean up temp file after interpreter is loaded
            NSFileManager.defaultManager.removeItemAtPath(tempPath, null)

            // Allocate tensors
            memScoped {
                println("TFLiteRunner iOS: Allocating tensors")
                val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                val allocated = interpreter?.allocateTensorsWithError(errorPtr.ptr)

                val error = errorPtr.value
                if (error != null || allocated != true) {
                    throw IllegalStateException(
                        "Failed to allocate tensors: ${error?.localizedDescription ?: "unknown error"}"
                    )
                }
            }

            // Validate single input
            val inputTensorCount = interpreter?.inputTensorCount?.toInt() ?: 0
            if (inputTensorCount != 1) {
                throw IllegalStateException("Model must have single input. Found: $inputTensorCount inputs")
            }

            // Cache INPUT information
            memScoped {
                println("TFLiteRunner iOS: Caching input tensor info")
                val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                val inputTensor = interpreter?.inputTensorAtIndex(0u, error = errorPtr.ptr)

                val error = errorPtr.value
                if (error != null || inputTensor == null) {
                    throw IllegalStateException(
                        "Failed to get input tensor: ${error?.localizedDescription ?: "unknown error"}"
                    )
                }

                // Get input shape
                val shapeErrorPtr = alloc<ObjCObjectVar<NSError?>>()
                val dims = inputTensor.shapeWithError(shapeErrorPtr.ptr) as? NSArray

                if (dims != null) {
                    inputShape = IntArray(dims.count.toInt()) { index ->
                        (dims.objectAtIndex(index.toULong()) as NSNumber).intValue
                    }
                }

                // Calculate input buffer size
                val dataErrorPtr = alloc<ObjCObjectVar<NSError?>>()
                inputBufferSize = inputTensor.dataWithError(dataErrorPtr.ptr)?.length?.toInt() ?: 0

                println("TFLiteRunner iOS: Input shape: ${inputShape?.contentToString()}")
                println("TFLiteRunner iOS: Input buffer size: $inputBufferSize bytes")
            }

            // Cache OUTPUT information (single or multiple)
            outputCount = interpreter?.outputTensorCount?.toInt() ?: 0
            println("TFLiteRunner iOS: Output count: $outputCount")

            val tempOutputShapes = mutableListOf<IntArray>()
            val tempOutputDataTypes = mutableListOf<TensorDataType>()
            val tempOutputBufferSizes = mutableListOf<Int>()
            val tempOutputNames = mutableListOf<String>()

            for (outputIndex in 0 until outputCount) {
                memScoped {
                    val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                    val outputTensor = interpreter?.outputTensorAtIndex(
                        outputIndex.toULong(),
                        error = errorPtr.ptr
                    )

                    val error = errorPtr.value
                    if (error != null || outputTensor == null) {
                        throw IllegalStateException(
                            "Failed to get output tensor at index $outputIndex: ${error?.localizedDescription ?: "unknown error"}"
                        )
                    }

                    // Get output tensor name
                    val tensorName = outputTensor.name
                    tempOutputNames.add(tensorName)

                    // Get output shape
                    val shapeErrorPtr = alloc<ObjCObjectVar<NSError?>>()
                    val dims = outputTensor.shapeWithError(shapeErrorPtr.ptr) as? NSArray

                    val shape = if (dims != null) {
                        IntArray(dims.count.toInt()) { index ->
                            (dims.objectAtIndex(index.toULong()) as NSNumber).intValue
                        }
                    } else {
                        intArrayOf()
                    }
                    tempOutputShapes.add(shape)

                    // Get output data type
                    val dataType = mapDataType(outputTensor.dataType)
                    tempOutputDataTypes.add(dataType)

                    // Calculate output buffer size
                    val dataErrorPtr = alloc<ObjCObjectVar<NSError?>>()
                    val bufferSize =
                        outputTensor.dataWithError(dataErrorPtr.ptr)?.length?.toInt() ?: 0
                    tempOutputBufferSizes.add(bufferSize)

                    println("TFLiteRunner iOS: Output[$outputIndex] shape: ${shape.contentToString()}, type: $dataType, size: $bufferSize bytes")
                    println("TFLiteRunner iOS: Output[$outputIndex] name: '$tensorName', shape: ${shape.contentToString()}, type: $dataType, size: $bufferSize bytes")
                }
            }

            outputShapes = tempOutputShapes
            outputDataTypes = tempOutputDataTypes
            outputBufferSizes = tempOutputBufferSizes
            outputNames = tempOutputNames

            // Create name to index mapping
            outputNameToIndexMap = tempOutputNames.withIndex().associate { (index, name) ->
                name to index
            }

            initialized = true
            println("TFLiteRunner iOS: ✅ Initialized successfully")

        } catch (e: Exception) {
            cleanup()
            throw IllegalStateException("Failed to initialize TFLite model: ${e.message}", e)
        }
    }

    private fun mapDataType(tfLiteType: TFLTensorDataType): TensorDataType {
        return when (tfLiteType) {
            TFLTensorDataType.TFLTensorDataTypeFloat32 -> TensorDataType.FLOAT32
            TFLTensorDataType.TFLTensorDataTypeUInt8 -> TensorDataType.UINT8
            TFLTensorDataType.TFLTensorDataTypeInt8 -> TensorDataType.INT8
            TFLTensorDataType.TFLTensorDataTypeInt32 -> TensorDataType.INT32
            TFLTensorDataType.TFLTensorDataTypeInt64 -> TensorDataType.INT64
            else -> TensorDataType.FLOAT32
        }
    }

    actual fun isInitialized(): Boolean = initialized

    @OptIn(BetaInteropApi::class)
    actual fun runInference(inputData: ByteArray): InferenceResult {
        if (!initialized || interpreter == null) {
            throw IllegalStateException("TFLiteRunner not initialized. Call initialize() first.")
        }

        try {
            // Validate input size (this check is good, keep it)
            if (inputData.size != inputBufferSize) {
                throw IllegalArgumentException(
                    "Input size mismatch. Expected: $inputBufferSize bytes, Got: ${inputData.size} bytes"
                )
            }

            // Copy Input Data to Tensor ---
            memScoped {
                val errorPtr = alloc<ObjCObjectVar<NSError?>>()

                // Get the input tensor
                val inputTensor = interpreter?.inputTensorAtIndex(0u, error = errorPtr.ptr)
                if (errorPtr.value != null || inputTensor == null) {
                    throw RuntimeException("Failed to get input tensor: ${errorPtr.value?.localizedDescription ?: "unknown error"}")
                }

                // Convert Kotlin ByteArray to NSData for the copy operation
                val inputNSData = inputData.usePinned { pinned ->
                    NSData.dataWithBytes(pinned.addressOf(0), inputData.size.toULong())
                }

                // Use the high-level TFLite API to copy data. This is safer than manual memcpy.
                inputTensor.copyData(inputNSData, errorPtr.ptr)
                if (errorPtr.value != null) {
                    throw RuntimeException("Failed to copy data to input tensor: ${errorPtr.value?.localizedDescription}")
                }
            }

            // Run Inference
            memScoped {
                val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                val invokeSuccess = interpreter?.invokeWithError(errorPtr.ptr)

                if (errorPtr.value != null || invokeSuccess == false) {
                    throw RuntimeException("Inference invocation failed: ${errorPtr.value?.localizedDescription ?: "unknown error"}")
                }
            }

            // Extract all outputs
            val outputs = mutableListOf<ByteArray>()

            for (outputIndex in 0 until outputCount) {
                val outputBytes = memScoped {
                    val errorPtr = alloc<ObjCObjectVar<NSError?>>()

                    val outputTensor = interpreter?.outputTensorAtIndex(
                        outputIndex.toULong(),
                        error = errorPtr.ptr
                    )
                    if (errorPtr.value != null || outputTensor == null) {
                        throw RuntimeException("Failed to get output tensor at index $outputIndex: ${errorPtr.value?.localizedDescription ?: "unknown error"}")
                    }

                    val outputNSData = outputTensor.dataWithError(errorPtr.ptr)
                    if (errorPtr.value != null || outputNSData == null) {
                        throw RuntimeException("Failed to read data from output tensor at index $outputIndex: ${errorPtr.value?.localizedDescription ?: "output data is null"}")
                    }

                    val outputSize = outputNSData.length.toInt()

                    ByteArray(outputSize).apply {
                        usePinned { pinned ->
                            memcpy(pinned.addressOf(0), outputNSData.bytes, outputSize.toULong())
                        }
                    }
                }

                outputs.add(outputBytes)
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

    /**
     * Get output tensor data by name after running inference
     * @param name The name of the output tensor
     * @return TensorData containing the output data, or null if not found
     */
    @OptIn(BetaInteropApi::class)
    actual fun getOutputByName(name: String): TensorData? {
        if (!initialized) {
            throw IllegalStateException("TFLiteRunner not initialized. Call initialize() first.")
        }

        memScoped {
            for (i in 0 until getOutputCount()) {
                val errorPtr = alloc<ObjCObjectVar<NSError?>>()

                // Get the output tensor
                val outputTensor = interpreter?.outputTensorAtIndex(
                    i.toULong(),
                    error = errorPtr.ptr
                )

                if (outputTensor?.name == name) {
                    val dataPtr = outputTensor.dataWithError(errorPtr.ptr)
                    if (errorPtr.value != null || dataPtr == null) {
                        return null
                    }
                    val dataSize = dataPtr.length.toInt()
                    val data = ByteArray(dataSize)

                    data.usePinned { pinned ->
                        memcpy(pinned.addressOf(0), dataPtr.bytes, dataSize.toULong())
                    }

                    return TensorData(
                        data = data,
                        scale = outputTensor.quantizationParameters?.scale ?: 1.0f,
                        zeroPoint = outputTensor.quantizationParameters?.zeroPoint ?: 0
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
        cleanup()
    }

    private fun cleanup() {
        try {
            interpreter = null
        } catch (e: Exception) {
            println("TFLiteRunner iOS: Error during cleanup: ${e.message}")
        } finally {
            inputShape = null
            inputBufferSize = 0
            outputShapes = null
            outputDataTypes = null
            outputBufferSizes = null
            outputNames = null
            outputNameToIndexMap = null
            outputCount = 0
            initialized = false
            println("TFLiteRunner iOS: Cleaned up")
        }
    }
}