package com.chaintech.faceliveness

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import com.chaintech.faceliveness.ui.CameraView
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.chaintech.faceliveness.base.FaceConfig
import com.chaintech.faceliveness.util.MaxBrightnessEffect
import kyc.facecontroller.generated.resources.Res
import kyc.facecontroller.generated.resources.close
import org.jetbrains.compose.resources.painterResource

@Composable
fun FaceLivenessScreen(
    faceConfig: FaceConfig = FaceConfig(),
    modifier: Modifier = Modifier,
    onProcessComplete: (image: ImageBitmap, imagePath: String, videoPath: String) -> Unit,
    onClose:()->Unit,
) {
    if(faceConfig.autoBrightnessIncreased) {
        MaxBrightnessEffect()
    }
    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Image(painter = painterResource(Res.drawable.close),
                contentDescription = "for close",
                modifier = Modifier
                    .padding(top = 20.dp,end = 20.dp)
                    .size(20.dp)
                    .clickable{
                    onClose()
                })
        }
        CameraView(
            faceConfig =  faceConfig,
            modifier = modifier,
            onProcessComplete = onProcessComplete,
            onClose = onClose
        )
    }

}


