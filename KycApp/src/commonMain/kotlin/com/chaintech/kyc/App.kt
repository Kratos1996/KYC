package com.chaintech.kyc

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.chaintech.kyc.di.appModule
import com.chaintech.kyc.theme.AppTheme
import com.chaintech.kyc.ui.HomeViewModel
import com.chaintech.kyc.core.navigation.KycNavHost
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import com.chaintech.faceliveness.di.commonModule
import com.chaintech.faceliveness.di.platformModule
import com.chaintech.kyc.core.KeepScreenOn

@Composable
@Preview
fun App(finishActivity:()->Unit) {
    val viewModel: HomeViewModel = koinInject()
    AppSetUp {

        KycNavHost(viewModel,finishActivity)
    }
}

@Composable
fun AppSetUp(content: @Composable () -> Unit) {
    AppTheme {
        KoinApplication(application = { modules(
            appModule(),
            commonModule(),
            platformModule()
        )
        }) {
            content()
        }
    }
}


