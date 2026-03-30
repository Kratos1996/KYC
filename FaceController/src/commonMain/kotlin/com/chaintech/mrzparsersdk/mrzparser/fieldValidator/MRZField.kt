package com.chaintech.mrzparsersdk.mrzparser.fieldValidator

// MARK: - MRZField
internal data class MRZField(
    val value: Any?,
    val rawValue: String,
    val checkDigit: String?,
    val isValid: Boolean
) {
    constructor(value: Any?, rawValue: String, checkDigit: String) : this(
        value = value,
        rawValue = rawValue,
        checkDigit = checkDigit,
        isValid = if (checkDigit.isEmpty()) {
            false
        } else {
            isValueValid(rawValue, checkDigit)
        }
    )

    companion object {
        fun isValueValid(value: String, checkDigit: String): Boolean {
            val numericCheckDigit = checkDigit.toIntOrNull()
            if (numericCheckDigit == null) {
                if (checkDigit == "<") {
                    return value.trimFillers().isEmpty()
                }
                return false
            }

            val weights = listOf(7, 3, 1)
            var total = 0

            value.forEachIndexed { index, char ->
                val charValue = when {
                    char in 'A'..'Z' -> char.code - 'A'.code + 10
                    char in '0'..'9' -> char.toString().toInt()
                    char == '<' -> 0
                    else -> return false
                }
                total += charValue * weights[index % 3]
            }

            return (total % 10 == numericCheckDigit)
        }
    }
}

// MARK: - String Extensions
fun String.trimFillers(): String {
//    return this.replace("<", "").trim()
    return this.trim('<')
}
