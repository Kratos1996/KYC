package com.chaintech.mrzparsersdk.mrzparser.extractionManager

import com.chaintech.mrzparsersdk.mrzparser.model.MRZException
import com.chaintech.mrzparsersdk.mrzparser.model.MRZResultModel
import com.chaintech.mrzparsersdk.mrzparser.parser.MRZCodeParser

internal class MRZExtractionManager {
    private val mrzParser = MRZCodeParser(ocrCorrection = true)

    suspend fun performMRZExtraction(
        recognizedString: String
    ): MRZResultModel {
        val mrzLines = mrzParser.parseMRZAndExtractLines(recognizedString)
            ?: throw MRZException.MrzCodeNotAvailable

        val mrzResult = mrzParser.parse(mrzLines)

        if (mrzResult != null) {
//            if (!mrzResult.allCheckDigitsValid) {
//                throw MRZError.InvalidDocument
//            }

//            if (mrzResult.isDocumentDateExpired() == true) {
//                throw MRZException.Custom("Document is expiry")
//            }

            return mrzResult
        } else {
            throw MRZException.MrzCodeNotAvailable
        }
    }
}