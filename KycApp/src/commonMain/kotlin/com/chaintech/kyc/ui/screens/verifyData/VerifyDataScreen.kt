package com.chaintech.kyc.ui.screens.verifyData

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.chaintech.kyc.core.navigation.AppScreenKyc
import com.chaintech.kyc.core.navigation.kycNavigation.Navigator
import com.chaintech.kyc.ui.HomeViewModel
import com.chaintech.kyc.ui.screens.loading.LoaderScreen
import com.chaintech.kyc.ui.screens.retry.RetryScreen
import com.chaintech.kyc.ui.screens.success.SuccessScreen
import com.chaintech.kyc.utility.model.LocationResult
import com.chaintech.kyc.utility.model.LocationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import network.chaintech.cmpeasypermission.PermissionState
import network.chaintech.cmpeasypermission.RequestPermission

@Composable
fun VerifyDataScreen(viewModel: HomeViewModel) {
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
                    CoroutineScope(Dispatchers.IO).launch {
                        viewModel.fetchCurrentLocation(onSuccess = {
                            viewModel.startDocumentCropping()
                        }){

                        }
                    }
                }
            },
        )
    }
    LaunchedEffect(Unit) {
        viewModel.fetchCurrentLocation(onSuccess = {
            viewModel.startDocumentCropping()
        }) { result ->
            when (result) {
                is LocationResult.PermissionDenied -> {
                    viewModel.errorMessage.value = "Please grant location permission"
                    viewModel.isError.value = true
                    startGps = true
                }

                is LocationResult.Error -> {
                    viewModel.errorMessage.value = "Error on Location Data. Please try again"
                    viewModel.isError.value = true
                }

                LocationResult.LocationDisabled -> {
                    viewModel.errorMessage.value = "Please enable location"
                    viewModel.isError.value = true
                }

                is LocationResult.Success -> {

                }
            }
        }
    }
    VerifyDataView(viewModel = viewModel)
}

@Composable
private fun VerifyDataView(viewModel: HomeViewModel) {
    when {
        viewModel.isProgressLoader.value -> {
            LoaderScreen()
        }

        viewModel.isError.value -> {
            RetryScreen(
                title = "Document Verification",
                message = viewModel.errorMessage.value,
            ) {
                if (viewModel.locationState.value?.permissionDenied == true) {
                    viewModel.openLocationSettings()
                } else {
                    viewModel.navigate(Navigator.NavigateBack)
                }
                // viewModel.navigate(Navigator.NavigateRoute(AppScreenKyc.SubmitScreen))
            }
        }

        viewModel.isSuccess.value -> {
            SuccessScreen(
                title = "Document Verification",
                message = "Document Verified Successfully"
            ) {
                viewModel.navigate(Navigator.NavigateRoute(AppScreenKyc.SubmitScreen))
            }
        }

        else -> {
            LoaderScreen()
        }
    }
}