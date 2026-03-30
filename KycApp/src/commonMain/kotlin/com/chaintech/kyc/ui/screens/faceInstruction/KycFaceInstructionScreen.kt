package com.chaintech.kyc.ui.screens.faceInstruction


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.layout.ContentScale
import androidx.constraintlayout.compose.ConstraintLayout
import com.chaintech.kyc.core.AppFont
import com.chaintech.kyc.core.FontType
import com.chaintech.kyc.core.navigation.AppScreenKyc
import com.chaintech.kyc.core.navigation.kycNavigation.Navigator
import com.chaintech.kyc.theme.KycAppColor
import com.chaintech.kyc.ui.components.KycToolBar
import com.chaintech.kyc.ui.components.PrimaryButton
import com.chaintech.kyc.ui.HomeViewModel
import kyc.kycapp.generated.resources.Res
import kyc.kycapp.generated.resources.doc_scanner
import kyc.kycapp.generated.resources.focus_camera
import kyc.kycapp.generated.resources.ic_face
import kyc.kycapp.generated.resources.light_ic
import network.chaintech.cmpeasypermission.PermissionState
import network.chaintech.cmpeasypermission.RequestPermission
import network.chaintech.sdpcomposemultiplatform.sdp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource


@Composable
fun KycFaceCameraInstructionScreen(viewModel: HomeViewModel) {
    var openCamera by remember { mutableStateOf(false) }
    var isGrantedCamera by remember { mutableStateOf<Boolean?>(null) }
    if (openCamera) {
        RequestPermission(
            permission = PermissionState.CAMERA,
            openSetting = true,
            deniedDialogTitle = "App requires access to your camera",
            deniedDialogDesc = "You can enable this permission in the settings",
            isGranted = { isGranted: Boolean ->
                isGrantedCamera = isGranted
                openCamera = false
                if(isGrantedCamera == true){
                    viewModel.navigate(Navigator.NavigateRoute(route = AppScreenKyc.FaceLivenessScreen))
                }
            },
        )
    }

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .background(KycAppColor.colors.background)
            .padding(horizontal = 8.sdp)
    ) {
        val (mainUiRef, btnUiRef) = createRefs()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .constrainAs(mainUiRef) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        )
        {
            KycToolBar(modifier = Modifier.fillMaxWidth(), title = "KYC Face Verification") {
                viewModel.navigate(
                    Navigator.NavigatePopBackStack(
                        AppScreenKyc.KycStartScreen,
                        inclusive = false
                    )
                )
            }
            Spacer(modifier = Modifier.height(24.sdp))
            // Face Detection Illustration
            Image(
                painter = painterResource(Res.drawable.ic_face),
                contentScale = ContentScale.Fit,
                contentDescription = "",
                modifier = Modifier.padding(top = 20.sdp).size(width = 100.sdp, height = 130.sdp)
            )
            Spacer(modifier = Modifier.height(32.sdp))
            // Face Detection Title and Description
            Text(
                text = "Face Detection",
                style = AppFont.roboto(
                    FontType.Heading,
                    AppFont.Roboto.Bold
                ),
                modifier = Modifier
                    .padding(start = 12.sdp, top = 12.sdp)
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.sdp))

            Text(
                text = "Scan your face to verify your identity.",
                style = AppFont.roboto(
                    FontType.SubHeading,
                    AppFont.Roboto.Regular,
                    color = KycAppColor.colors.secondaryText,
                ),
                modifier = Modifier
                    .padding(start = 12.sdp, top = 4.sdp)
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.sdp))

            // Guidelines
            Column(verticalArrangement = Arrangement.spacedBy(16.sdp)) {
                GuidelineItem(
                    iconId = Res.drawable.focus_camera, // Replace with your icon resource
                    text = "Keep your background clear"
                )
                GuidelineItem(
                    iconId = Res.drawable.light_ic, // Replace with your icon resource
                    text = "Have a good brightness to clear your face"
                )
                GuidelineItem(
                    iconId = Res.drawable.doc_scanner, // Replace with your icon resource
                    text = "Center your face in the circle"
                )
            }

        }
        PrimaryButton(
            title = "Continue",
            modifier = Modifier
                .padding(top = 10.sdp, bottom = 10.sdp)
                .fillMaxWidth()
                .constrainAs(btnUiRef) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                }
        ) {
           openCamera = true
        }
    }
}

@Composable
fun GuidelineItem(
    iconId: DrawableResource,
    text: String
) {
    Row(modifier = Modifier.padding(horizontal = 12.sdp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.sdp)
    ) {
        Icon(
            painter = painterResource( iconId), // Replace with your icon resource
            contentDescription = null,
            tint = Color(0xFF6C63FF),
            modifier = Modifier.size(24.sdp)
        )
        Text(
            text = text,
            style = AppFont.roboto(
                FontType.Regular,
                AppFont.Roboto.Regular
            ),
        )
    }
}