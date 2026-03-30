package com.chaintech.faceliveness.di

import com.chaintech.faceliveness.util.FaceLandmarkerHelper
import com.chaintech.faceliveness.util.FacePoseCalculator
import com.chaintech.faceliveness.util.ImageCaptureHelper
import com.chaintech.faceliveness.util.VideoRecorder
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    // Platform-specific helpers
    single { VideoRecorder(context = get()) }
    single { ImageCaptureHelper(context = get()) }
    single { FaceLandmarkerHelper(context = get()) }
    single { FacePoseCalculator() }
}
