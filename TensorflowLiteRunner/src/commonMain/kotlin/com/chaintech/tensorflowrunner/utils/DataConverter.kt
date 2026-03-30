package com.chaintech.tensorflowrunner.utils

/**
 * Platform-specific data conversion utilities
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object DataConverter {
    fun floatArrayToByteArray(floatArray: FloatArray): ByteArray
    fun byteArrayToFloatArray(byteArray: ByteArray): FloatArray
}