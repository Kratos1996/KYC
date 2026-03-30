package com.chaintech.kyc.di

import com.chaintech.facecropper.FaceCropper
import com.chaintech.kyc.core.KeepScreenOn
import com.chaintech.kyc.utility.location.LocationHelper
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun specialModule(): Module = module {
    single { FaceCropper() }
    single { KeepScreenOn() }
    single { LocationHelper() }
}