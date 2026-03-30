package com.chaintech.kyc.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chaintech.kyc.moduleTest.docFieldDetector.DocumentFieldDetectorDemo
import com.chaintech.kyc.moduleTest.docdetector.DocumentDetectorDemo
import com.chaintech.kyc.moduleTest.faceAntiSpoofingDemo.FaceAntiSpoofingDemo
import com.chaintech.kyc.moduleTest.faceMatchingDemo.FaceMatchingDemo
import com.chaintech.kyc.moduleTest.mrzParserDemo.MrzParserDemo
import com.chaintech.kyc.theme.KycAppColor

@Composable
fun SDKModuleTestView() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .background(KycAppColor.colors.background)
                    .safeContentPadding()
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // MRZParserDemo
                MrzParserDemo()

                // DocumentFieldDetectorDemo
                DocumentFieldDetectorDemo()

                // FaceMatchingDemo
                FaceMatchingDemo()

                //FaceAntiSpoofingDemo
                FaceAntiSpoofingDemo()

                //DocumentDetectorDemo
                DocumentDetectorDemo()
            }
        }
    }
}