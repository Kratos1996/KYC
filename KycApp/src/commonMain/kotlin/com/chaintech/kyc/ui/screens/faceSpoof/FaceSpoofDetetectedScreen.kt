package com.chaintech.kyc.ui.screens.faceSpoof

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.chaintech.kyc.core.navigation.AppScreenKyc
import com.chaintech.kyc.core.navigation.kycNavigation.Navigator
import com.chaintech.kyc.ui.HomeViewModel
import com.chaintech.kyc.ui.screens.loading.LoaderScreen
import com.chaintech.kyc.ui.screens.retry.RetryScreen
import com.chaintech.kyc.ui.screens.success.SuccessScreen

@Composable
fun FaceSpoofDetectionScreen(viewModel: HomeViewModel) {
    LaunchedEffect(Unit){
        viewModel.startWorkOnFaceData()
    }
    DisposableEffect(Unit){
        onDispose {
            viewModel.resetAllData()
        }
    }

    when {
        viewModel.isProgressLoader.value -> {
            LoaderScreen()
        }
        viewModel.isError.value -> {
            RetryScreen(
                title = "Face Verification",
                message = viewModel.errorMessage.value
            ) {

                viewModel.navigate(Navigator.NavigateBack)
            }
        }
        viewModel.isSuccess.value -> {
            SuccessScreen(
                title = "Face Verification",
                message = "Face Verified Successfully"
            ) {
                viewModel.navigate(Navigator.NavigateRoute(AppScreenKyc.KycStartScreen))
            }
        }
        else -> {
            LoaderScreen()
        }
    }
}