package com.chaintech.kyc.moduleTest.tesseractDemo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chaintech.kyc.moduleTest.docdetector.CroppedImageView
import com.chaintech.mrzparsersdk.mrzparser.MRZParserSDK
import kotlinx.coroutines.launch
import kyc.kycapp.generated.resources.Res
import kyc.kycapp.generated.resources.TempMrzImage_1
import org.jetbrains.compose.resources.imageResource

@Composable
fun TesseractAndMrzParserDemo() {
    val scope = rememberCoroutineScope()
    val mrzImage = imageResource(Res.drawable.TempMrzImage_1)
    val mrzService = remember { MRZParserSDK.createMRZExtractionService() }
    val isButtonTap = remember { mutableStateOf(false) }
    val ocrString = remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .safeContentPadding()
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(onClick = {
            scope.launch {
                try {
                    mrzService.initialize()
                    val result = mrzService.parseMrz(mrzImage)
                    println("OCR Result -> $result")
                    isButtonTap.value = true
                    if (result != null) {
                        println("********************")
                        println("mrzType: ${result.mrzType}")
                        println("documentType: ${result.documentType}")
                        println("countryCode: ${result.countryCode}")
                        println("documentNumber: ${result.documentNumber}")
                        println("birthdate: ${result.birthdate}")
                        println("sex: ${result.sex}")
                        println("expiryDate: ${result.expiryDate}")
                        println("nationalityCountryCode: ${result.nationalityCountryCode}")
                        println("surnames: ${result.surnames}")
                        println("givenNames: ${result.givenNames}")
                        println("optionalData: ${result.optionalData}")
                        println("optionalData2: ${result.optionalData2}")
                        println("********************")
                    }
                    mrzService.dispose()
                } catch (e: Exception) {
                    println("❌ MrzParser Unexpected error: ${e.message}")
                }
            }
        }) {
            Text("Tesseract Demo")
        }

        if (isButtonTap.value) {
            CroppedImageView(
                mrzImage,
                modifier = Modifier
                    .size(width = 200.dp, height = 200.dp)
                    .padding(horizontal = 5.dp)
            )
            Text("Score:  ${ocrString.value}")
        }
    }
}

/*
  val scope = rememberCoroutineScope()
    val mrzImage = imageResource(Res.drawable.TempMrzImage_1)
    val ocrManager = remember { createTesseractManager() }
    val mrzService = remember { MRZParserSDK.createMRZExtractionService() }
    val isButtonTap = remember { mutableStateOf(false) }
    val ocrString = remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .safeContentPadding()
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(onClick = {
            scope.launch {
                try {
                    ocrManager.initialize("mrz")
                    ocrManager.setPageSegmentationMode(PageSegmentationMode.AUTO)

                    val result = ocrManager.extractTextFromBitmap(mrzImage)
                    println("OCR Result -> $result")
                    ocrManager.release()
                    isButtonTap.value = true
                    if (result != null) {
                        ocrString.value = result

                        val result = mrzService.processMRZ(recognizedString = result)

                        println("********************")
                        println("mrzType: ${result.mrzType}")
                        println("documentType: ${result.documentType}")
                        println("countryCode: ${result.countryCode}")
                        println("documentNumber: ${result.documentNumber}")
                        println("birthdate: ${result.birthdate}")
                        println("sex: ${result.sex}")
                        println("expiryDate: ${result.expiryDate}")
                        println("nationalityCountryCode: ${result.nationalityCountryCode}")
                        println("surnames: ${result.surnames}")
                        println("givenNames: ${result.givenNames}")
                        println("optionalData: ${result.optionalData}")
                        println("optionalData2: ${result.optionalData2}")
                        println("********************")
                    }
                } catch (e: Exception) {
                    println("❌ Face Matching Unexpected error: ${e.message}")
                }
            }
        }) {
            Text("Tesseract Demo")
        }

        if (isButtonTap.value) {
            CroppedImageView(
                mrzImage,
                modifier = Modifier
                    .size(width = 200.dp, height = 200.dp)
                    .padding(horizontal = 5.dp)
            )
            Text("Score:  ${ocrString.value}")
        }
    }
 */