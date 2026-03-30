package com.chaintech.mrzparsersdk.mrzparser.fieldValidator

import com.chaintech.mrzparsersdk.mrzparser.model.MRZFieldType

internal class MRZFieldFormatter(private val ocrCorrection: Boolean) {
    // MARK: Main
    fun field(
        fieldType: MRZFieldType,
        from: String,
        startIndex: Int,
        length: Int,
        checkDigitFollows: Boolean = false
    ): MRZField {
        val endIndex = startIndex + length
        var rawValue = from.safeSubstring(startIndex, endIndex - 1)
        var checkDigit = if (checkDigitFollows) from.safeSubstring(endIndex, endIndex) else ""

        if (ocrCorrection) {
            rawValue = correct(rawValue, fieldType)
            checkDigit = if (checkDigit.isEmpty()) "" else correct(checkDigit, fieldType)
        }

        return MRZField(
            value = format(rawValue, fieldType),
            rawValue = rawValue,
            checkDigit = checkDigit
        )
    }

    fun format(string: String, fieldType: MRZFieldType): Any? {
        return when (fieldType) {
            MRZFieldType.Names -> names(string)
            MRZFieldType.Birthdate -> birthdate(string)
            MRZFieldType.Sex -> sex(string)
            MRZFieldType.ExpiryDate -> expiryDate(string)
            else -> text(string)
        }
    }

    fun correct(string: String, fieldType: MRZFieldType): String {
        return when (fieldType) {
            MRZFieldType.Birthdate, MRZFieldType.ExpiryDate, MRZFieldType.Hash ->
                replaceLetters(string)
            MRZFieldType.Names, MRZFieldType.DocumentType, MRZFieldType.CountryCode, MRZFieldType.Nationality ->
                replaceDigits(string)
            MRZFieldType.Sex -> string.replace("P", "F")
            else -> string
        }
    }

    // MARK: Value Formatters
    private fun names(string: String): Pair<String, String> {
        val identifiers = string.trimFillers()
            .split("<<")
            .map { it.replace("<", " ") }
        val primary = identifiers.getOrNull(0) ?: ""
        val secondary = identifiers.getOrNull(1) ?: ""
        return Pair(primary, secondary)
    }

    private fun sex(string: String): String? {
        return when (string) {
            "M" -> "MALE"
            "F" -> "FEMALE"
            "<" -> "UNSPECIFIED"
            else -> null
        }
    }

    private fun text(string: String): String {
        return string.trimFillers().replace("<", " ")
    }

    // MARK: Utils
    private fun replaceDigits(string: String): String {
        return string
            .replace("0", "O")
            .replace("1", "I")
            .replace("2", "Z")
            .replace("8", "B")
    }

    private fun replaceLetters(string: String): String {
        return string
            .replace("O", "0")
            .replace("Q", "0")
            .replace("U", "0")
            .replace("D", "0")
            .replace("I", "1")
            .replace("Z", "2")
            .replace("B", "8")
    }

    private fun birthdate(string: String): String {
        if (!string.all { it.isDigit() } || string.length < 6) return ""

        return string
    }

    private fun expiryDate(string: String): String {
        if (!string.all { it.isDigit() } || string.length < 6) return ""
        return string
    }
    /*
    @OptIn(ExperimentalTime::class)
    private fun birthdate(string: String): String {
        if (!string.all { it.isDigit() } || string.length < 6) return ""

        val currentYear = Clock.System.now().toLocalDateTime(TimeZone.UTC).year - 2000
        val parsedYear = string.substring(0, 2).toIntOrNull() ?: return ""

        val century = if (parsedYear > currentYear) 1900 else 2000
        val fullYear = century + parsedYear

        return try {
            val month = string.substring(2, 4).toIntOrNull() ?: return ""
            val day = string.substring(4, 6).toIntOrNull() ?: return ""

            LocalDate(fullYear, month, day).toString() // Returns yyyy-MM-dd format
        } catch (e: Exception) {
            ""
        }
    }

    private fun expiryDate(string: String): String {
        if (!string.all { it.isDigit() } || string.length < 6) return ""

        val parsedYear = string.substring(0, 2).toIntOrNull() ?: return ""
        val century = if (parsedYear >= 70) 1900 else 2000
        val fullYear = century + parsedYear

        return try {
            val month = string.substring(2, 4).toIntOrNull() ?: return ""
            val day = string.substring(4, 6).toIntOrNull() ?: return ""

            LocalDate(fullYear, month, day).toString() // Returns yyyy-MM-dd format
        } catch (e: Exception) {
            ""
        }
    }
     */
}

// MARK: - String Extensions
private fun String.safeSubstring(startIndex: Int, endIndex: Int): String {
    return if (startIndex in indices && endIndex in indices && startIndex <= endIndex) {
        this.substring(startIndex, endIndex + 1)
    } else {
        ""
    }
}
