package com.chaintech.faceliveness.util

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreImage.CIContext
import platform.CoreImage.CIImage
import platform.CoreImage.createCGImage
import kotlinx.cinterop.*
import org.jetbrains.skia.Image
import platform.CoreGraphics.*
import platform.CoreMedia.*
import platform.CoreVideo.*
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
class OrientationListener(
    val orientationChanged: (UIDeviceOrientation) -> Unit
) : NSObject() {
    private val notificationName = UIDeviceOrientationDidChangeNotification

    @OptIn(BetaInteropApi::class)
    @Suppress("UNUSED_PARAMETER")
    @ObjCAction
    fun orientationDidChange(arg: NSNotification) {
        orientationChanged(UIDevice.currentDevice.orientation)
    }

    fun register() {
        NSNotificationCenter.defaultCenter.addObserver(
            observer = this,
            selector = NSSelectorFromString(
                OrientationListener::orientationDidChange.name + ":"
            ),
            name = notificationName,
            `object` = null
        )
    }

    fun unregister() {
        NSNotificationCenter.defaultCenter.removeObserver(
            observer = this,
            name = notificationName,
            `object` = null
        )
    }
}

// Extension function to convert UIImage to ImageBitmap
fun UIImage.toImageBitmap(): ImageBitmap {
    // 1. Convert UIImage to NSData (PNG or JPEG)
    val data = UIImagePNGRepresentation(this)
        ?: UIImageJPEGRepresentation(this, 1.0) // 1.0 is highest quality
        ?: error("Could not get image data from UIImage")

    // 2. Convert NSData to a Kotlin ByteArray
    val bytes = data.toByteArray()

    // 3. Decode the ByteArray into a Skia Image and then to a Compose ImageBitmap
    return Image.makeFromEncoded(bytes).toComposeImageBitmap()
}

// Helper function we created before, useful to have here
@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val bytes = ByteArray(size)
    if (size > 0) {
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this.bytes, this.length)
        }
    }
    return bytes
}


fun currentFormattedTime(): String {
    val dateFormatter = NSDateFormatter()
    dateFormatter.dateFormat = "HH:mm:ss"
    return dateFormatter.stringFromDate(NSDate())
}