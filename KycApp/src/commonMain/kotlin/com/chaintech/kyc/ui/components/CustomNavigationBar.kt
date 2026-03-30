package com.chaintech.kyc.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.chaintech.kyc.core.AppFont
import com.chaintech.kyc.core.FontType
import com.chaintech.kyc.theme.KycAppColor
import kyc.kycapp.generated.resources.Res
import kyc.kycapp.generated.resources.ic_backButtonIcon
import org.jetbrains.compose.resources.painterResource

@Composable
fun CustomNavigationBar(
    title: String,
    onBack: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp) // Standard AppBar height
            .background(KycAppColor.colors.background)
            .shadow(
                elevation = 2.dp,
                ambientColor = Color.Black.copy(alpha = 0.1f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            )
            .padding(horizontal = 24.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Back button (optional)
            if (onBack != null) {
                Image(
                    painter = painterResource(Res.drawable.ic_backButtonIcon),
                    contentDescription = "Back",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onBack() },
                )
            }

            // 🧾 Title
            Text(
                text = title,
                style = AppFont.sfProSans(
                    FontType.Heading,
                    AppFont.SfProSans.Medium
                ),
                color = KycAppColor.colors.primaryText,
                modifier = Modifier.padding(start = 13.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // if back button exists
            if (onBack != null) {
                Spacer(modifier = Modifier.width(40.dp))
            }
        }
    }
}