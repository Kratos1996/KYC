package com.chaintech.mrzparsersdk.mrzparser.parser

import com.chaintech.mrzparsersdk.mrzparser.fieldValidator.MRZFieldFormatter
import com.chaintech.mrzparsersdk.mrzparser.mrzFormatter.MRZFormat
import com.chaintech.mrzparsersdk.mrzparser.mrzFormatter.MRZFormatParser
import com.chaintech.mrzparsersdk.mrzparser.model.MRZResultModel
import kotlin.collections.iterator

internal class MRZCodeParser(
    ocrCorrection: Boolean = false
) {

    private val formatter: MRZFieldFormatter = MRZFieldFormatter(ocrCorrection)

    // --- Parsing ---
    internal fun parse(mrzLines: List<String>): MRZResultModel? {
        val mrzFormat = mrzFormat(mrzLines)

        return when (mrzFormat) {
            MRZFormat.TD1 -> MRZFormatParser(mrzLines, formatter, MRZFormat.TD1).result
            MRZFormat.TD2 -> MRZFormatParser(mrzLines, formatter, MRZFormat.TD2).result
            MRZFormat.TD3 -> MRZFormatParser(mrzLines, formatter, MRZFormat.TD3).result
            MRZFormat.INVALID -> null
        }
    }

    // --- MRZ-Format detection ---
    private fun mrzFormat(mrzLines: List<String>): MRZFormat {
        return when (mrzLines.size) {
            MRZFormat.TD2.linesCount, MRZFormat.TD3.linesCount -> {
                val lineLength = uniformedLineLength(mrzLines)
                val possibleFormats = mapOf(
                    MRZFormat.TD2 to MRZFormat.TD2.lineLength,
                    MRZFormat.TD3 to MRZFormat.TD3.lineLength
                )

                for ((format, requiredLength) in possibleFormats) {
                    if (lineLength == requiredLength) return format
                }
                MRZFormat.INVALID
            }
            MRZFormat.TD1.linesCount -> {
                if (uniformedLineLength(mrzLines) == MRZFormat.TD1.lineLength) MRZFormat.TD1 else MRZFormat.INVALID
            }
            else -> MRZFormat.INVALID
        }
    }

    private fun uniformedLineLength(mrzLines: List<String>): Int? {
        val lineLength = mrzLines.firstOrNull()?.length ?: return null
        return if (mrzLines.any { it.length != lineLength }) null else lineLength
    }

    // --- Extract MRZ lines from recognized OCR text ---
    internal fun parseMRZAndExtractLines(recognizedText: String): List<String>? {
        val td1Pattern = "([A-Z0-9<]{30}\\n[A-Z0-9<]{30}\\n[A-Z0-9<]{30})"  // TD1: 3 lines of 30 chars
        val td2Pattern = "([A-Z0-9<]{36}\\n[A-Z0-9<]{36})"                  // TD2: 2 lines of 36 chars
        val td3Pattern = "([A-Z0-9<]{44}\\n[A-Z0-9<]{44})"                  // TD3 / MRV-A: 2 lines of 44 chars
        val mrvaPattern = td3Pattern
        val mrvbPattern = td2Pattern

        val combinedPattern = "$td1Pattern|$td2Pattern|$td3Pattern|$mrvaPattern|$mrvbPattern"

        try {
            // Normalize input similarly to the Swift code
            val normalizedText = recognizedText
                .replace(" ", "")               // remove spaces (keep newlines)
                .trim()
                .replace("\n\n", "\n")
                .uppercase()

            val regex = Regex(combinedPattern)
            val matches = regex.findAll(normalizedText).toList()
            if (matches.isEmpty()) return null

            val mrzLines = matches.flatMap { match ->
                match.value
                    .replace(" ", "")
                    .split("\n")
                    .filter { it.isNotEmpty() }
            }

            val validLines = mrzLines.filter { it.length == 30 || it.length == 36 || it.length == 44 }
            return validLines.ifEmpty { null }
        } catch (e: Exception) {
            return null
        }
    }
}
