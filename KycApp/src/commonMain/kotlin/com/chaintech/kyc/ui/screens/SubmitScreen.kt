package com.chaintech.kyc.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.chaintech.documentcontroller.documentRectDetector.dataModels.DocumentRectClassName
import com.chaintech.kyc.core.navigation.AppScreenKyc
import com.chaintech.kyc.core.navigation.kycNavigation.Navigator
import com.chaintech.kyc.ui.HomeViewModel
import com.chaintech.kyc.ui.components.KycToolBar
import com.chaintech.kyc.utility.model.IPValues
import network.chaintech.sdpcomposemultiplatform.sdp

@Composable
fun SubmitScreen(kycViewModel: HomeViewModel){
    DisposableEffect(Unit) {
        onDispose {
            kycViewModel.resetDataForReKyc()
        }
    }
    KycVerificationDetailScreen(kycViewModel = kycViewModel)
}
@Composable
private fun KycVerificationDetailScreen(
    kycViewModel: HomeViewModel,
) {
    val showExplayer = remember { mutableStateOf(false) }
    Column(
        modifier =
            Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .padding(bottom = 40.sdp)
                .background(Color.White),
    ) {
        // Top AppBar
        KycToolBar(modifier = Modifier.fillMaxWidth(), title = "KYC Verification Details") {
            kycViewModel.navigate(
                Navigator.NavigatePopBackStack(
                    AppScreenKyc.KycStartScreen,
                    inclusive = false,
                ),
            )
        }

        Column(
            modifier =
                Modifier
                    .background(color = Color(0xffF9F9F9))
                    .padding(horizontal = 16.sdp)
                    .padding(bottom = 16.sdp),
        ) {
            Spacer(modifier = Modifier.height(16.sdp))

            Text(
                text = "Uploaded Document",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black,
                fontSize = 22.sp,
            )
            Spacer(modifier = Modifier.height(16.sdp))

            Image(
                bitmap = kycViewModel.getScannedDocument()[0].croppedBitmap, // Replace with your document image
                contentDescription = "Uploaded Document",
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(200.sdp)
                        .clip(RoundedCornerShape(8.sdp)),
                contentScale = ContentScale.Fit,
            )

            if (kycViewModel.getScannedDocument().size == 2) {
                Spacer(modifier = Modifier.height(16.sdp))
                Image(
                    bitmap = kycViewModel.getScannedDocument()[1].croppedBitmap, // Replace with your document image
                    contentDescription = "Uploaded Document",
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(200.sdp)
                            .clip(RoundedCornerShape(8.sdp)),
                    contentScale = ContentScale.Fit,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.sdp))

        HorizontalDivider(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(2.sdp),
        )

        Spacer(modifier = Modifier.height(16.sdp))

        // Your Photos Section
        Text(
            text = "Your Photos",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.sdp),
            color = Color.Black,
            fontSize = 22.sp,
        )

        Spacer(modifier = Modifier.height(16.sdp))

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.sdp),
            horizontalArrangement = Arrangement.spacedBy(16.sdp),
        ) {
            // Image from Document
            kycViewModel.getCroppedImagesFromDocument(DocumentRectClassName.PHOTO.value)?.let { selfie ->
                ImageCard(
                    modifier = Modifier.weight(1f),
                    title = "Image from Document",
                    bitmap = selfie, // Replace with your image resource
                )
            }

            kycViewModel.faceSelfieBitmap.value?.let {
                ImageCard(
                    modifier = Modifier.weight(1f),
                    title = "Selfie",
                    bitmap = it, // Replace with your image resource
                )
            }
        }

        Spacer(modifier = Modifier.height(24.sdp))

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.sdp),
            horizontalArrangement = Arrangement.spacedBy(16.sdp),
        ) {
            // "Signature from Document"
            kycViewModel.getCroppedImagesFromDocument(DocumentRectClassName.SIGNATURE.value)?.let {
                ImageCard(
                    modifier = Modifier.weight(1f),
                    title = "Signature from Document",
                    bitmap = it, // Replace with your image resource
                )
            }

            // "QR Code"
            kycViewModel.getCroppedImagesFromDocument(DocumentRectClassName.QR_CODE.value)?.let {
                ImageCard(
                    modifier = Modifier.weight(1f),
                    title = "QR Code",
                    bitmap = it, // Replace with your image resource
                )
            }
        }

        Spacer(modifier = Modifier.height(24.sdp))

        HorizontalDivider(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(2.sdp),
        )

        Spacer(modifier = Modifier.height(24.sdp))
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.sdp),
        ) {
            Text(
                modifier = Modifier,
                text = "Your Id Details",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black,
                fontSize = 22.sp,
            )

            Spacer(modifier = Modifier.height(8.sdp))

            HorizontalDivider(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(2.sdp),
            )

            Spacer(modifier = Modifier.height(8.sdp))

            kycViewModel.showScanDocumentDetails().fastForEach { (title, titleValue) ->
                Spacer(modifier = Modifier.height(5.sdp))
                IDDetailRow(title, titleValue)
                Spacer(modifier = Modifier.height(5.sdp))
                DashedDivider()
            }

            Spacer(modifier = Modifier.height(24.sdp))

            Text(
                modifier = Modifier,
                text = "Your Network details",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Black,
                fontSize = 22.sp,
            )
            Spacer(modifier = Modifier.height(8.sdp))
            HorizontalDivider(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(2.sdp),
            )
            Spacer(modifier = Modifier.height(8.sdp))

            IDDetailRow("country", IPValues.ipInfo?.country ?: "")
            Spacer(modifier = Modifier.height(8.sdp))
            IDDetailRow("ip", IPValues.ipInfo?.ip.toString() + "/" + IPValues.publicIp)
            Spacer(modifier = Modifier.height(8.sdp))
            IDDetailRow("network", IPValues.ipInfo?.network ?: "")
            Spacer(modifier = Modifier.height(8.sdp))
            IDDetailRow("version", IPValues.ipInfo?.version ?: "")
            Spacer(modifier = Modifier.height(8.sdp))
            IDDetailRow("city", IPValues.ipInfo?.city ?: "")
            Spacer(modifier = Modifier.height(8.sdp))
            IDDetailRow("region", IPValues.ipInfo?.region ?: "")
            Spacer(modifier = Modifier.height(8.sdp))
            IDDetailRow("country_name", IPValues.ipInfo?.country_name ?: "")
            Spacer(modifier = Modifier.height(8.sdp))
            IDDetailRow(
                "latitude",
                (IPValues.ipInfo?.latitude ?: "").toString() + "/" + IPValues.lattitude,
            )
            Spacer(modifier = Modifier.height(8.sdp))
            IDDetailRow(
                "longitude",
                (IPValues.ipInfo?.longitude ?: "").toString() + "/" + IPValues.longnitude,
            )
            Spacer(modifier = Modifier.height(8.sdp))
            IDDetailRow("postal", IPValues.ipInfo?.postal ?: "")
            Spacer(modifier = Modifier.height(8.sdp))

            DashedDivider()
            Spacer(modifier = Modifier.height(8.sdp))

            IDDetailRow("Anti Spoofing Score",kycViewModel.antiSpoofResult.value?.antiSpoofingScore.toString())
            Spacer(modifier = Modifier.height(8.sdp))

            IDDetailRow("Face Similarity  Score", kycViewModel.faceMatchingResult.value?.similarity.toString())
            Spacer(modifier = Modifier.height(8.sdp))

            DashedDivider()
            IDDetailRow("Face Token", kycViewModel.faceToken.value?.take(20).toString())

            Spacer(modifier = Modifier.height(8.sdp))

            kycViewModel.locationState.value?.location?.let { location ->
                HorizontalDivider(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(2.sdp),
                )
                Text(
                    modifier = Modifier,
                    text = "Your Location details",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Black,
                    fontSize = 22.sp,
                )
                IDDetailRow("Latitude", location.latitude.toString())
                Spacer(modifier = Modifier.height(8.sdp))
                DashedDivider()
                Spacer(modifier = Modifier.height(8.sdp))
                IDDetailRow("Longitude", location.longitude.toString())
                Spacer(modifier = Modifier.height(8.sdp))
                DashedDivider()
                Spacer(modifier = Modifier.height(8.sdp))
                IDDetailRow("Address",  location.address.toString())
                Spacer(modifier = Modifier.height(8.sdp))
                DashedDivider()
                IDDetailRow("City",  location.city.toString())
                DashedDivider()
                Spacer(modifier = Modifier.height(8.sdp))
                IDDetailRow("State",  location.state.toString())
                Spacer(modifier = Modifier.height(8.sdp))
                DashedDivider()
                IDDetailRow("Country", location.country.toString())
                Spacer(modifier = Modifier.height(8.sdp))
                DashedDivider()
                IDDetailRow("Postal Code", location.postalCode.toString())
                Spacer(modifier = Modifier.height(8.sdp))
                DashedDivider()
                }
        }
        Button(
            onClick = {
                kycViewModel.navigate(
                    Navigator.NavigatePopBackStack(
                        AppScreenKyc.KycStartScreen,
                        inclusive = false,
                    ),
                )
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.sdp)
                    .height(48.sdp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759)),
        ) {
            Text(
                text = "Re-KYC",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
   /* if (showExplayer.value) {
        ExoPlayerScreen(
            context = context,
            modifier = Modifier.fillMaxSize(),
            uri = kycViewModel.identityIdentifier.videoUrl,
            viewModel = kycViewModel,
        )
    }*/
}

@Composable
fun ImageCard(
    modifier: Modifier,
    title: String,
    bitmap: ImageBitmap,
) {
    Column(
        modifier =
            modifier
                .background(Color.White)
                .clip(RoundedCornerShape(8.sdp))
                .border(1.sdp, Color.Gray, RoundedCornerShape(8.sdp))
                .padding(8.sdp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = title,
            modifier =
                Modifier
                    .size(100.sdp)
                    .clip(RoundedCornerShape(8.sdp)),
            contentScale = ContentScale.Fit,
        )
        Spacer(modifier = Modifier.height(8.sdp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun IDDetailRow(
    label: String,
    value: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.sdp, horizontal = 4.sdp),
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
        )
        Text(
            modifier = Modifier.weight(1f),
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Black,
        )
    }
}

@Composable
fun DashedDivider(
    color: Color = Color.Gray,
    thickness: Dp = 1.sdp,
    dashLength: Float = 10f,
    gapLength: Float = 10f,
) {
    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(thickness),
    ) {
        val stroke =
            Stroke(
                width = thickness.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLength, gapLength), 0f),
            )
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = thickness.toPx(),
            pathEffect = stroke.pathEffect,
        )
    }
}