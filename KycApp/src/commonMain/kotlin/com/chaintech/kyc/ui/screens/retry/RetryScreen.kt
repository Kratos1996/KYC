package com.chaintech.kyc.ui.screens.retry

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.chaintech.kyc.ui.components.PrimaryButton
import kyc.kycapp.generated.resources.Res
import kyc.kycapp.generated.resources.red_cross
import network.chaintech.sdpcomposemultiplatform.sdp
import network.chaintech.sdpcomposemultiplatform.ssp
import org.jetbrains.compose.resources.painterResource

@Composable
fun RetryScreen(title: String, message: String = "Please try again", btnTxt:String ="Retry", onclick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FB)) // Background color
            .padding(start = 8.sdp, end = 8.sdp, bottom = 50.sdp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(80.sdp))

        Icon(
            painter = painterResource(Res.drawable.red_cross), // Replace with your check icon drawable
            contentDescription = "Verified",
            modifier = Modifier.size(90.sdp)
        )


        Spacer(modifier = Modifier.height(24.sdp))

        // Verifying Data Text
        Text(
            modifier = Modifier.padding(horizontal = 16.sdp),
            text = title,
            fontSize = 20.ssp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212121),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.sdp))

        // Subtext
        Text(
            text = message,
            fontSize = 14.ssp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF757575),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))
        // Okay Button
        PrimaryButton(title = btnTxt, modifier = Modifier) {
            onclick()
        }
    }
}