package com.chaintech.kyc.di

import com.chaintech.documentcontroller.docdetector.DocumentDetectorSDK
import com.chaintech.documentcontroller.documentRectDetector.DocumentFieldDetectorSDK
import com.chaintech.faceantispoofing.FaceAntiSpoofingSDK
import com.chaintech.faceliveness.util.ImageCaptureHelper
import com.chaintech.facematching.FaceMatchingSDK
import com.chaintech.facetoken.FaceTokenSDK
import com.chaintech.googlemlkitocr.createTextRecognizer
import com.chaintech.kyc.network.HttpClient
import com.chaintech.kyc.network.IpRepository
import com.chaintech.kyc.network.ApiService
import com.chaintech.kyc.network.api.IpApiService
import com.chaintech.kyc.ui.HomeViewModel
import com.chaintech.kyc.utility.AppStrings
import com.chaintech.mrzparsersdk.mrzparser.MRZParserSDK
import com.chaintech.kyc.utility.location.LocationRepository
import org.koin.core.module.Module
import org.koin.dsl.module

fun appModule() = module {
    factory { AppStrings() }
    factory { HttpClient() }
    single { ApiService(baseUrl = "https://ipapi.co/", httpClient = get()) }
    single { IpApiService(apiService = get()) }
    single { IpRepository(ipApiService = get()) }
    single {
        HomeViewModel(
            appString = get(),
            documentDetectorService = get(),
            documentFieldDetectorService = get(),
            faceCropper = get(),
            faceAntiSpoofingService = get(),
            faceMatchingService = get(),
            ipRepository = get(),
            textRecognizer = get(),
            mrzParserService = get(),
            faceTokenService = get(),
            locationRepository = get()
        )
    }
    single { DocumentDetectorSDK.createDetectorService() }
    single { DocumentFieldDetectorSDK.createDetectorService() }
    single { FaceAntiSpoofingSDK.createDetectorService() }
    single { FaceMatchingSDK.createDetectorService() }
    single { createTextRecognizer() }
    single { MRZParserSDK.createMRZExtractionService() }
    single { FaceTokenSDK.createFaceTokenService() }
    single { LocationRepository(locationHelper = get()) }

}

expect fun specialModule(): Module

