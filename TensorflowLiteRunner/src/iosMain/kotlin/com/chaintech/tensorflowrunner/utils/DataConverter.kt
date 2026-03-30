@file:OptIn(ExperimentalForeignApi::class)

package com.chaintech.tensorflowrunner.utils

import kotlinx.cinterop.*
import platform.posix.memcpy

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object DataConverter {
    actual fun floatArrayToByteArray(floatArray: FloatArray): ByteArray {
        return memScoped {
            val byteArray = ByteArray(floatArray.size * 4)
            floatArray.usePinned { pinnedFloat ->
                byteArray.usePinned { pinnedByte ->
                    memcpy(
                        pinnedByte.addressOf(0),
                        pinnedFloat.addressOf(0),
                        (floatArray.size * 4).toULong()
                    )
                }
            }
            byteArray
        }
    }

    actual fun byteArrayToFloatArray(byteArray: ByteArray): FloatArray {
        return memScoped {
            val floatArray = FloatArray(byteArray.size / 4)
            byteArray.usePinned { pinnedByte ->
                floatArray.usePinned { pinnedFloat ->
                    memcpy(
                        pinnedFloat.addressOf(0),
                        pinnedByte.addressOf(0),
                        byteArray.size.toULong()
                    )
                }
            }
            floatArray
        }
    }
}