package com.chaintech.kyc.core.navigation

import kotlinx.serialization.Serializable

sealed class AppScreenKyc {
    @Serializable
    object KycStartScreen

    @Serializable
    object ScannerScreen

    @Serializable
    object VerifyDataScreen
    @Serializable
    object FaceInstructionScreen

    @Serializable
    object FaceLivenessScreen

    @Serializable
    object VerifyingFaceDataScreen

    @Serializable
    object SubmitScreen
}
