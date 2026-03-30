package com.chaintech.mrzparsersdk.mrzparser.model

// MARK: - MRZFieldType
internal enum class MRZFieldType {
    DocumentType,
    CountryCode,
    Names,
    DocumentNumber,
    Nationality,
    Birthdate,
    Sex,
    ExpiryDate,
    OptionalData,
    OptionalData2,
    Hash
}

// --- Error Handling ---
sealed class MRZException(message: String) : Exception(message) {
    object InvalidDocument : MRZException("The document is invalid. Please try again.")
    object MrzCodeNotAvailable : MRZException("MRZ code could not be found or recognized.")
    object CheckDigitsValidationFailed : MRZException("MRZ code check digits validation failed.")
    class Custom(val customMessage: String) : MRZException(customMessage)
}

// --- SDK Output Model ---

// --- Internal Parsed MRZ Model (used by parser) ---
data class MRZResultModel(
    val documentType: String,
    val countryCode: String,
    val surnames: String,
    val givenNames: String,
    val documentNumber: String,
    val nationalityCountryCode: String,
    val birthdate: String?,          // null if formatting failed
    val sex: String?,                // null if formatting failed
    val expiryDate: String?,         // null if formatting failed
    val optionalData: String?,       // null if not provided
    val optionalData2: String?,      // null if not provided
    val mrzType: String,

    val isDocumentNumberValid: Boolean,
    val isBirthdateValid: Boolean,
    val isExpiryDateValid: Boolean,
    val allCheckDigitsValid: Boolean,
) {
    /*
    @OptIn(ExperimentalTime::class)
    fun isDocumentDateExpired(
        possibleFormats: List<String> = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd",
            "MM/dd/yyyy",
            "dd/MM/yyyy"
        )
    ): Boolean? {
        val dateString = this.expiryDate ?: return null

        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        for (format in possibleFormats) {
            try {
                val parsedDate = when {
                    format.contains("HH:mm:ss") -> {
                        // Parse as datetime, extract date
                        LocalDateTime.parse(dateString.replace(" ", "T")).date
                    }

                    format == "yyyy-MM-dd" -> {
                        LocalDate.parse(dateString)
                    }

                    format == "MM/dd/yyyy" -> {
                        val parts = dateString.split("/")
                        if (parts.size == 3) {
                            LocalDate(parts[2].toInt(), parts[0].toInt(), parts[1].toInt())
                        } else continue
                    }

                    format == "dd/MM/yyyy" -> {
                        val parts = dateString.split("/")
                        if (parts.size == 3) {
                            LocalDate(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
                        } else continue
                    }

                    else -> continue
                }

                return parsedDate < now
            } catch (e: Exception) {
                // Try next format
                continue
            }
        }

        return null // No format worked
    }
     */
}
