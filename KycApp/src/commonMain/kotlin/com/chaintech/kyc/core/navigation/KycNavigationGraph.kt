package com.chaintech.kyc.core.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.chaintech.documentcontroller.scanner.DocumentScanner
import com.chaintech.faceliveness.FaceLivenessScreen
import com.chaintech.faceliveness.base.FaceConfig
import com.chaintech.faceliveness.base.FaceMoveDetection
import com.chaintech.faceliveness.base.FaceTask
import com.chaintech.kyc.ui.HomeViewModel
import com.chaintech.kyc.ui.screens.SubmitScreen
import com.chaintech.kyc.ui.screens.faceInstruction.KycFaceCameraInstructionScreen
import com.chaintech.kyc.ui.screens.faceSpoof.FaceSpoofDetectionScreen
import com.chaintech.kyc.ui.screens.homeview.KycStartScreen
import com.chaintech.kyc.ui.screens.verifyData.VerifyDataScreen

@Composable
fun KycNavigationGraph(
    modifier: Modifier,
    navController: NavHostController,
    homeViewModel: HomeViewModel
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = AppScreenKyc.FaceInstructionScreen,
        enterTransition = {
            fadeIn(animationSpec = tween(500))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(500))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(500))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(500))
        },
    ) {
        // first Screen
        composable<AppScreenKyc.KycStartScreen> {
            KycStartScreen(kycViewModel = homeViewModel)
        }
        // IDScanning Screen
        composable<AppScreenKyc.ScannerScreen> {
            DocumentScanner(
                noOfDocuments = homeViewModel.loadDocumnetScanner.value.noOfDocuments,
                modifier = homeViewModel.loadDocumnetScanner.value.modifier,
                overlayColor = homeViewModel.loadDocumnetScanner.value.overlayColor ,
                overlayBorderColor = homeViewModel.loadDocumnetScanner.value.overlayBorderColor,
                overlayBorderRoundedCornerColor = homeViewModel.loadDocumnetScanner.value.overlayBorderRoundedCornerColor,
                onCompletion = {
                    homeViewModel.documentScanningComplete(it)
                },
                permissionDeniedView = {
                    homeViewModel.permissionDeniedView()
                }
            )
        }
        // passport scanner
        composable<AppScreenKyc.VerifyDataScreen> {
            VerifyDataScreen(viewModel = homeViewModel)
        }
        composable<AppScreenKyc.FaceInstructionScreen> {
            KycFaceCameraInstructionScreen(viewModel = homeViewModel)
        }
        // when we come from ID Screen Scanning
        composable<AppScreenKyc.FaceLivenessScreen> {
            FaceLivenessScreen (
                faceConfig = FaceConfig(
                    totalEyeCount = 3,
                    listOfFaceDetection = listOf(
                        FaceMoveDetection.MOVE_LEFT, FaceMoveDetection.MOVE_RIGHT
                    ),
                    listOfFaceTask = listOf(
                        FaceTask.FACE_ROTATION,
                        FaceTask.EYE_BLINK,
                        FaceTask.SELFIE_CAPTURE
                    ),
                    listOfFaceDiagonalDetection = listOf()
                ),
                onProcessComplete = { imagePath, videoPath, bitmaps ->
                homeViewModel.faceLivenessComplete(imagePath, videoPath, bitmaps)
            }){
                homeViewModel.navigateBack()
            }
        }
        // Once Scanning Done then verify the data
        composable<AppScreenKyc.VerifyingFaceDataScreen> {
            FaceSpoofDetectionScreen(homeViewModel)
        }


        // Selfie Camera Scanner
        composable<AppScreenKyc.SubmitScreen> {
           /* val mrzData = it.toRoute<AppScreenKyc.CameraWithFace>()
            mrzData.mrz?.let {
                CameraScreen(it)
            }*/
            SubmitScreen(kycViewModel = homeViewModel)
        }

    }
}
