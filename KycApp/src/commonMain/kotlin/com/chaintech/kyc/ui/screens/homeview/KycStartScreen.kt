package com.chaintech.kyc.ui.screens.homeview

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaintech.kyc.core.AppFont
import com.chaintech.kyc.core.FontType
import com.chaintech.kyc.core.navigation.AppScreenKyc
import com.chaintech.kyc.core.navigation.kycNavigation.Navigator
import com.chaintech.kyc.theme.KycAppColor
import com.chaintech.kyc.ui.HomeViewModel
import com.chaintech.kyc.ui.components.CustomNavigationBar
import com.chaintech.kyc.ui.HomeViewModel.DocumentScanner
import kyc.kycapp.generated.resources.Res
import kyc.kycapp.generated.resources.ic_arrow
import kyc.kycapp.generated.resources.ic_docPlaceholderIcon
import kyc.kycapp.generated.resources.ic_drivingLicenseIcon
import kyc.kycapp.generated.resources.ic_idCardIcon
import kyc.kycapp.generated.resources.ic_passportIcon
import network.chaintech.cmpeasypermission.PermissionState
import network.chaintech.cmpeasypermission.RequestPermission
import network.chaintech.sdpcomposemultiplatform.sdp
import network.chaintech.sdpcomposemultiplatform.ssp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun KycStartScreen(kycViewModel: HomeViewModel) {
    var startGps by remember { mutableStateOf(false) }
    var isGrantedGps by remember { mutableStateOf<Boolean?>(null) }
    if (startGps) {
        RequestPermission(
            permission = PermissionState.ACCESS_FINE_LOCATION,
            openSetting = true,
            deniedDialogTitle = "App requires access to your location",
            deniedDialogDesc = "You can enable this permission in the settings",
            isGranted = { isGranted: Boolean ->
                isGrantedGps = isGranted
                startGps = false
                if(isGrantedGps == true){
                    kycViewModel.navigate(Navigator.NavigateRoute(AppScreenKyc.ScannerScreen))
                }
            },
        )
    }
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(KycAppColor.colors.background),
        horizontalAlignment = Alignment.CenterHorizontally,
    )
    {
        // Top Bar

        CustomNavigationBar(title = kycViewModel.appString.kycVerification)

        // Illustration Placeholder
        Spacer(modifier = Modifier.height(24.sdp))

        Image(
            painter = painterResource(Res.drawable.ic_docPlaceholderIcon),
            contentDescription = "KYC Illustration",
            modifier = Modifier.size(120.sdp),
        )

        Spacer(modifier = Modifier.height(30.sdp))
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.sdp),
        ) {
            // Heading
            Text(
                text = kycViewModel.appString.uploadProofTitle,
                fontSize = 16.ssp,
                fontWeight = FontWeight.Bold,
                style = AppFont.sfProSans(
                    FontType.Heading,
                    AppFont.SfProSans.Bold
                )
            )
            Text(
                text = kycViewModel.appString.uploadProofSubtitle,
                fontSize = 12.ssp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 12.sdp),
                style = AppFont.roboto(
                    FontType.Regular,
                    AppFont.Roboto.Regular
                )
            )
        }
        Spacer(modifier = Modifier.height(24.sdp))

        // Document Options
        DocumentOption(
            drawable = Res.drawable.ic_idCardIcon,
            title = kycViewModel.appString.idCard
        ) {
            kycViewModel.loadDocumnetScanner.value = DocumentScanner(
                modifier = Modifier.fillMaxSize(),
                documentType = "ID",
                noOfDocuments = 2
            )
            startGps = true

            //kycViewModel.navigate(Navigator.NavigateRoute(AppScreenKyc.ScannerScreen))
        }
        DocumentOption(
            drawable = Res.drawable.ic_passportIcon,
            title = kycViewModel.appString.passport
        ) {
            kycViewModel.loadDocumnetScanner.value = DocumentScanner(
                modifier = Modifier.fillMaxSize(),
                documentType = "PASSPORT",
                noOfDocuments = 1
            )
            startGps = true
           // kycViewModel.navigate(Navigator.NavigateRoute(AppScreenKyc.ScannerScreen))
        }
        DocumentOption(
            drawable = Res.drawable.ic_drivingLicenseIcon,
            title = kycViewModel.appString.drivingLicense,
        ) {
            kycViewModel.loadDocumnetScanner.value = DocumentScanner(
                modifier = Modifier.fillMaxSize(),
                documentType = "DRIVING_LICENSE",
                noOfDocuments = 2
            )
            startGps = true
            kycViewModel.navigate(Navigator.NavigateRoute(AppScreenKyc.ScannerScreen))
        }

        Spacer(modifier = Modifier.weight(1f))
        // Need Help?
        Text(
            text = kycViewModel.appString.needHelp,
            color = Color.Black,
            style = TextStyle(
                fontSize = 14.sp, // Adjust based on your subHeading size
                fontWeight = FontWeight.Normal,
                color = KycAppColor.colors.primaryText,
                textDecoration = TextDecoration.Underline
            ),
            modifier =
                Modifier
                    .clickable { /* Open Help Section */ }
                    .padding(vertical = 16.dp),
        )
    }
}




@Composable
fun DocumentOption(
    drawable: DrawableResource,
    title: String,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.sdp, vertical = 8.sdp)
                .clickable { onClick() },
        shape = RoundedCornerShape(8.sdp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.sdp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.sdp, vertical = 12.sdp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(40.sdp)
                        .background(color = Color(0XFFF9F9F9), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(drawable),
                    contentDescription = title,
                    modifier = Modifier.size(22.sdp),
                )
            }
            Spacer(modifier = Modifier.width(16.sdp))
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                color = KycAppColor.colors.primaryText,
                style = AppFont.productSans(
                    size = FontType.Heading, // Adjust based on your subHeading size
                    type = AppFont.ProductSans.Medium
                ),
            )
            Icon(
                painter = painterResource(Res.drawable.ic_arrow),
                contentDescription = "Next",
                tint = Color(0xFF333334),
                modifier = Modifier.size(10.sdp),
            )
        }
    }
}
