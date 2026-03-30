package com.chaintech.kyc.ui.screens.loading

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.chaintech.kyc.core.AppFont
import com.chaintech.kyc.core.FontType
import com.chaintech.kyc.theme.KycAppColor
import com.chaintech.kyc.ui.components.RotatingLoader
import network.chaintech.sdpcomposemultiplatform.sdp

@Composable
fun LoaderScreen(title:String = "Verifying Data",message: String = "Just a moment, we are verifying your information.") {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(KycAppColor.colors.background) // Background color
                .padding(start = 8.sdp, end = 10.sdp, bottom = 50.sdp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // toolbar

        // Verification Icon
        Box(
            modifier =
                Modifier
                    .padding(top = 60.sdp)
                    .size(200.sdp)
                    .background(KycAppColor.colors.background, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            RotatingLoader()
        }

        // Verifying Data Text
        Text(
            text = title,
            style = AppFont.sfProSans(
                FontType.Heading,
                AppFont.SfProSans.Bold
            ),
            modifier = Modifier.padding(top = 60.sdp),
        )

        Spacer(modifier = Modifier.height(8.sdp))

        // Subtext
        Text(
            text = message,
            style = AppFont.roboto(
                FontType.Regular,
                AppFont.Roboto.Regular
            ),
        )
    }
}