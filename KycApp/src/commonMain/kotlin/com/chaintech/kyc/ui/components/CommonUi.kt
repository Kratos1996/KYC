package com.chaintech.kyc.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chaintech.kyc.core.AppFont
import com.chaintech.kyc.core.FontType
import com.chaintech.kyc.theme.KycAppColor
import kotlinx.coroutines.launch
import kyc.kycapp.generated.resources.Res
import kyc.kycapp.generated.resources.ic_backButtonIcon
import kyc.kycapp.generated.resources.inner_loader_ico
import kyc.kycapp.generated.resources.outer_loader_ico
import network.chaintech.sdpcomposemultiplatform.sdp
import network.chaintech.sdpcomposemultiplatform.ssp
import org.jetbrains.compose.resources.painterResource

@Composable
fun KycToolBar(title:String="",modifier: Modifier,onBack: () -> Unit) {
    Row(
        modifier = modifier
            .background(KycAppColor.colors.white),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = {onBack()}) {
            Icon(
                painter = painterResource(Res.drawable.ic_backButtonIcon),
                contentDescription = "Back",
            )
        }
        Text(
            text = title,
            style = AppFont.productSans(
                size = FontType.SubHeading, // Adjust based on your subHeading size
                type = AppFont.ProductSans.Medium
            ),
            modifier = Modifier
                .padding(start = 4.sdp)
                .weight(1f),
            textAlign = TextAlign.Start
        )
        Spacer(modifier = Modifier.width(48.sdp)) // To balance the IconButton
    }
}
@Composable
fun ButtonBorder(
    modifier: Modifier = Modifier,
    title: String,
    backgroundColor: Color = KycAppColor.colors.white,
    borderColor: Color = KycAppColor.colors.border,
    height: Dp = 50.sdp,
    onClick: () -> Unit,
    ) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RectangleShape)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(12.sdp)
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.sdp)
            )
            .clickable {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 16.ssp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}

@Composable
fun PrimaryButton(  title: String,modifier: Modifier,onClick: () -> Unit,){
    Button(
        onClick = {
           onClick()
        },
        colors = ButtonDefaults.buttonColors(containerColor = KycAppColor.colors.successGreenColor),
        modifier = modifier
            .fillMaxWidth()
            .height(40.sdp),
        shape = RoundedCornerShape(12.sdp)
    ) {
        Text(
            text = title,
            fontSize = 14.ssp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun RotatingLoader() {
    val outerRotation = remember { Animatable(0f) }
    val innerRotation = remember { Animatable(0f) }

    // Animate rotation forever
    LaunchedEffect(Unit) {
        launch {
            while (true) {
                outerRotation.animateTo(
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(3000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    )
                )
            }
        }

        launch {
            while (true) {
                innerRotation.animateTo(
                    targetValue = -360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(3000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    )
                )
            }
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .wrapContentWidth()
            .wrapContentHeight()
    ) {
        val color =  KycAppColor.colors.successGreenColor
        // Loader Circle (background stroke)
        Canvas(modifier = Modifier.size(170.dp)) {
            drawCircle(
                color = color.copy(alpha = 0.3f),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Outer image
        Image(
            painter = painterResource( Res.drawable.outer_loader_ico),
            contentDescription = null,
            modifier = Modifier
                .size(140.dp)
                .graphicsLayer {
                    rotationZ = outerRotation.value
                }
        )

        // Inner image
        Image(
            painter = painterResource( Res.drawable.inner_loader_ico),
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .graphicsLayer {
                    rotationZ = innerRotation.value
                }
        )
    }
}
