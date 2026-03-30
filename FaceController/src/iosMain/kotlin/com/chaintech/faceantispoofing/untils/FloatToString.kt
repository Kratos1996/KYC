@file:OptIn(ExperimentalForeignApi::class)

package com.chaintech.faceantispoofing.untils

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle

actual fun Float.toPlainString(maxDecimalPlaces: Int): String {
    val formatter = NSNumberFormatter()
    formatter.numberStyle = NSNumberFormatterDecimalStyle
    formatter.minimumFractionDigits = 0u
    formatter.maximumFractionDigits = maxDecimalPlaces.toULong()
    formatter.usesGroupingSeparator = false
    return formatter.stringFromNumber(NSNumber(this.toDouble())) ?: this.toString()
}