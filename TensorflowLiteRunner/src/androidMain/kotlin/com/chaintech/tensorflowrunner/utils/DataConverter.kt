package com.chaintech.tensorflowrunner.utils
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object DataConverter {
    actual fun floatArrayToByteArray(floatArray: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(floatArray.size * 4)
        buffer.order(ByteOrder.nativeOrder())
        buffer.asFloatBuffer().put(floatArray)
        return buffer.array()
    }

    actual fun byteArrayToFloatArray(byteArray: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(byteArray)
        buffer.order(ByteOrder.nativeOrder())
        val floatBuffer = buffer.asFloatBuffer()
        val floatArray = FloatArray(floatBuffer.remaining())
        floatBuffer.get(floatArray)
        return floatArray
    }
}