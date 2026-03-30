package com.chaintech.faceantispoofing.untils
import java.math.BigDecimal
import java.math.RoundingMode

actual fun Float.toPlainString(maxDecimalPlaces: Int): String {
    return BigDecimal(this.toDouble())
        .setScale(maxDecimalPlaces, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
}