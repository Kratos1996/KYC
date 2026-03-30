package com.chaintech.kyc.moduleTest.mrzParserDemo

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.chaintech.mrzparsersdk.mrzparser.MRZParserSDK
import kotlinx.coroutines.launch

@Composable
fun MrzParserDemo() {
    val scope = rememberCoroutineScope()
    val mrzService = remember { MRZParserSDK.createMRZExtractionService() }

    Button(onClick = {
        scope.launch {
//            val mrzString ="""
//                    ILARE1229132950784198875210522
//                    8801317M2406237SYR<<<<<<<<<<<1
//                    CHBIB<<EHSAN<MAJED<<<<<<<<<<<<
//                    """.trimIndent()
//
//            try {
//                val result = mrzService.processMRZ(recognizedString = mrzString)
//
//                println("********************")
//                println("mrzType: ${result.mrzType}")
//                println("documentType: ${result.documentType}")
//                println("countryCode: ${result.countryCode}")
//                println("documentNumber: ${result.documentNumber}")
//                println("birthdate: ${result.birthdate}")
//                println("sex: ${result.sex}")
//                println("expiryDate: ${result.expiryDate}")
//                println("nationalityCountryCode: ${result.nationalityCountryCode}")
//                println("surnames: ${result.surnames}")
//                println("givenNames: ${result.givenNames}")
//                println("optionalData: ${result.optionalData}")
//                println("optionalData2: ${result.optionalData2}")
//                println("********************")
//            } catch (e: Exception) {
//                println("❌ Unexpected error: ${e.message}")
//            }
        }
    }) {
        Text("Parse MRZ String into Model")
    }
}
