package com.chaintech.faceliveness.di

import com.chaintech.faceliveness.util.FaceLandmarkerHelper
import com.chaintech.faceliveness.util.FacePoseCalculator
import com.chaintech.faceliveness.util.ImageCaptureHelper
import com.chaintech.faceliveness.util.VideoRecorder
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    // Platform-specific helpers for iOS
    single { VideoRecorder() }
    single { ImageCaptureHelper() }
    single { FaceLandmarkerHelper() }
    single { FacePoseCalculator() }

}