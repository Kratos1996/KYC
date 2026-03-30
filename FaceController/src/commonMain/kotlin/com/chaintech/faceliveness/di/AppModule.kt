package com.chaintech.faceliveness.di
import com.chaintech.faceliveness.ui.FaceDetectionViewModel
import com.chaintech.faceliveness.util.*
import com.chaintech.facialAttributeDetection.FacialAttributeDetectionSession
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Common DI module - shared across all platforms
 */
fun commonModule() = module {
    // ViewModel
    // ✅ ViewModel
    single {
        FaceDetectionViewModel(
            videoRecorder = get(),
            imageCaptureHelper = get(),
            faceLandmarkerHelper = get(),
            facePoseCalculator = get(),
            facialAttributeDetectionSession = get()
        )
    }
    single {
        FacialAttributeDetectionSession(
            faceCropper = get()
        )
    }
}



/**
 * Platform-specific modules defined with expect/actual
 */
expect fun platformModule(): Module
