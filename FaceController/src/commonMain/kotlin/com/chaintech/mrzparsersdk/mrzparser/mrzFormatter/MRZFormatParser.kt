package com.chaintech.mrzparsersdk.mrzparser.mrzFormatter

import com.chaintech.mrzparsersdk.mrzparser.model.MRZFieldType
import com.chaintech.mrzparsersdk.mrzparser.model.MRZResultModel
import com.chaintech.mrzparsersdk.mrzparser.fieldValidator.MRZField
import com.chaintech.mrzparsersdk.mrzparser.fieldValidator.MRZFieldFormatter

// --- MRZ Format Enum ---
internal enum class MRZFormat(val lineLength: Int, val linesCount: Int) {
    TD1(30, 3),
    TD2(36, 2),
    TD3(44, 2),
    INVALID(0, 0)
}

// --- MRZ Format Parser ---
internal class MRZFormatParser(
    mrzLines: List<String>,
    formatter: MRZFieldFormatter,
    mrzFormat: MRZFormat
) {
    private val finalCheckDigit: String

    private val documentType: MRZField
    private val countryCode: MRZField
    private val documentNumber: MRZField
    private val optionalData: MRZField
    private val birthdate: MRZField
    private val sex: MRZField
    private val expiryDate: MRZField
    private val nationality: MRZField
    private val optionalData2: MRZField
    private val names: MRZField
    private val mrzType: String
    private val mrzFormatType: MRZFormat = mrzFormat

    // --- Check Digit Validation ---
    private val allCheckDigitsValid: Boolean by lazy {
        when (mrzFormatType) {
            MRZFormat.TD1 -> {
                val compositedValue = listOf(documentNumber, optionalData, birthdate, expiryDate, optionalData2)
                    .joinToString("") { it.rawValue + (it.checkDigit ?: "") }
                val isCompositeValid = MRZField.isValueValid(compositedValue, finalCheckDigit)
                documentNumber.isValid && birthdate.isValid && expiryDate.isValid && isCompositeValid
            }
            MRZFormat.TD2 -> {
                if (finalCheckDigit.isEmpty()) {
                    documentNumber.isValid && birthdate.isValid && expiryDate.isValid
                } else {
                    val compositedValue = listOf(documentNumber, birthdate, expiryDate, optionalData)
                        .joinToString("") { it.rawValue + (it.checkDigit ?: "") }
                    val isCompositeValid = MRZField.isValueValid(compositedValue, finalCheckDigit)
                    documentNumber.isValid && birthdate.isValid && expiryDate.isValid && isCompositeValid
                }
            }
            MRZFormat.TD3 -> {
                if (finalCheckDigit.isEmpty()) {
                    documentNumber.isValid && birthdate.isValid && expiryDate.isValid
                } else {
                    val compositeFields = mutableListOf(documentNumber, birthdate, expiryDate)
                    optionalData.let { compositeFields.add(it) }

                    val compositedValue = compositeFields
                        .joinToString("") { it.rawValue + (it.checkDigit ?: "") }
                    val isCompositeValid = MRZField.isValueValid(compositedValue, finalCheckDigit)

                    documentNumber.isValid &&
                            birthdate.isValid &&
                            expiryDate.isValid &&
                            optionalData.isValid &&
                            isCompositeValid
                }
            }
            else -> false
        }
    }

    // --- Result Model ---
    val result: MRZResultModel by lazy {
        val (surnames, givenNames) = (names.value as? Pair<*, *>) ?: ("" to "")

        MRZResultModel(
            documentType = documentType.value as? String ?: "",
            countryCode = countryCode.value as? String ?: "",
            surnames = surnames as String,
            givenNames = givenNames as String,
            documentNumber = documentNumber.value as? String ?: "",
            nationalityCountryCode = nationality.value as? String ?: "",
            birthdate = birthdate.value as? String ?: "",
            sex = sex.value as? String ?: "",
            expiryDate = expiryDate.value as? String ?: "",
            optionalData = optionalData.value as? String ?: "",
            optionalData2 = optionalData2.value as? String ?: "",
            isDocumentNumberValid = documentNumber.isValid,
            isBirthdateValid = birthdate.isValid,
            isExpiryDateValid = expiryDate.isValid,
            allCheckDigitsValid = allCheckDigitsValid,
            mrzType = mrzType,
        )
    }

    init {
        when (mrzFormat) {
            MRZFormat.TD1 -> {
                val (firstLine, secondLine, thirdLine) = Triple(mrzLines[0], mrzLines[1], mrzLines[2])

                documentType = formatter.field(MRZFieldType.DocumentType, firstLine, 0, 2)
                countryCode = formatter.field(MRZFieldType.CountryCode, firstLine, 2, 3)
                documentNumber = formatter.field(MRZFieldType.DocumentNumber, firstLine, 5, 9, true)
                optionalData = formatter.field(MRZFieldType.OptionalData, firstLine, 15, 15)

                birthdate = formatter.field(MRZFieldType.Birthdate, secondLine, 0, 6, true)
                sex = formatter.field(MRZFieldType.Sex, secondLine, 7, 1)
                expiryDate = formatter.field(MRZFieldType.ExpiryDate, secondLine, 8, 6, true)
                nationality = formatter.field(MRZFieldType.Nationality, secondLine, 15, 3)
                optionalData2 = formatter.field(MRZFieldType.OptionalData2, secondLine, 18, 11)
                finalCheckDigit = formatter.field(MRZFieldType.Hash, secondLine, 29, 1).rawValue

                names = formatter.field(MRZFieldType.Names, thirdLine, 0, 29)
                mrzType = "TD1"
            }

            MRZFormat.TD2 -> {
                val (firstLine, secondLine) = Pair(mrzLines[0], mrzLines[1])
                val isVisaDocument = firstLine.startsWith("V") // MRV-B

                documentType = formatter.field(MRZFieldType.DocumentType, firstLine, 0, 2)
                countryCode = formatter.field(MRZFieldType.CountryCode, firstLine, 2, 3)
                names = formatter.field(MRZFieldType.Names, firstLine, 5, 31)

                documentNumber = formatter.field(MRZFieldType.DocumentNumber, secondLine, 0, 9, true)
                nationality = formatter.field(MRZFieldType.Nationality, secondLine, 10, 3)
                birthdate = formatter.field(MRZFieldType.Birthdate, secondLine, 13, 6, true)
                sex = formatter.field(MRZFieldType.Sex, secondLine, 20, 1)
                expiryDate = formatter.field(MRZFieldType.ExpiryDate, secondLine, 21, 6, true)
                optionalData = formatter.field(MRZFieldType.OptionalData, secondLine, 28, if (isVisaDocument) 8 else 7)
                finalCheckDigit = if (isVisaDocument) "" else formatter.field(MRZFieldType.Hash, secondLine, 35, 1).rawValue

                // OptionalData2 will be null but here we just init they object
                optionalData2 = formatter.field(MRZFieldType.OptionalData2, "", 0, 0)

                mrzType = if (isVisaDocument) "MRV-B" else "TD2"
            }

            MRZFormat.TD3 -> {
                val (firstLine, secondLine) = Pair(mrzLines[0], mrzLines[1])
                val isVisaDocument = firstLine.startsWith("V") // MRV-A

                documentType = formatter.field(MRZFieldType.DocumentType, firstLine, 0, 2)
                countryCode = formatter.field(MRZFieldType.CountryCode, firstLine, 2, 3)
                names = formatter.field(MRZFieldType.Names, firstLine, 5, 39)

                documentNumber = formatter.field(MRZFieldType.DocumentNumber, secondLine, 0, 9, true)
                nationality = formatter.field(MRZFieldType.Nationality, secondLine, 10, 3)
                birthdate = formatter.field(MRZFieldType.Birthdate, secondLine, 13, 6, true)
                sex = formatter.field(MRZFieldType.Sex, secondLine, 20, 1)
                expiryDate = formatter.field(MRZFieldType.ExpiryDate, secondLine, 21, 6, true)

                if (isVisaDocument) {
                    optionalData = formatter.field(MRZFieldType.OptionalData, secondLine, 28, 16)
                    finalCheckDigit = ""
                } else {
                    optionalData = formatter.field(MRZFieldType.OptionalData, secondLine, 28, 14, true)
                    finalCheckDigit = formatter.field(MRZFieldType.Hash, secondLine, 43, 1).rawValue
                }

                optionalData2 = formatter.field(MRZFieldType.OptionalData2, "", 0, 0)
                mrzType = if (isVisaDocument) "MRV-A" else "TD3"
            }

            else -> {
                finalCheckDigit = ""
                documentType = formatter.field(MRZFieldType.DocumentType, "", 0, 0)
                countryCode = formatter.field(MRZFieldType.CountryCode, "", 0, 0)
                documentNumber = formatter.field(MRZFieldType.DocumentNumber, "", 0, 0, true)
                optionalData = formatter.field(MRZFieldType.OptionalData, "", 0, 0)
                birthdate = formatter.field(MRZFieldType.Birthdate, "", 0, 0, true)
                sex = formatter.field(MRZFieldType.Sex, "", 0, 0)
                expiryDate = formatter.field(MRZFieldType.ExpiryDate, "", 0, 0, true)
                nationality = formatter.field(MRZFieldType.Nationality, "", 0, 0)
                optionalData2 = formatter.field(MRZFieldType.OptionalData2, "", 0, 0)
                names = formatter.field(MRZFieldType.Names, "", 0, 0)
                mrzType = ""
            }
        }
    }
}

