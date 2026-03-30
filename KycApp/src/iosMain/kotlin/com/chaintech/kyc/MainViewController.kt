package com.chaintech.kyc

import androidx.compose.ui.window.ComposeUIViewController
import com.chaintech.faceliveness.di.commonModule
import com.chaintech.faceliveness.di.platformModule
import com.chaintech.kyc.core.KeepScreenOn
import com.chaintech.kyc.di.specialModule
import com.chaintech.kyc.di.appModule
import org.koin.core.context.startKoin

fun MainViewController() = ComposeUIViewController {
    val screenOn = KeepScreenOn()
    startKoin {
        modules(
            appModule(),
            specialModule(),
            commonModule(),
            platformModule()
        )
    }
    screenOn.enable()
    App{
        screenOn.disable()
    }
}