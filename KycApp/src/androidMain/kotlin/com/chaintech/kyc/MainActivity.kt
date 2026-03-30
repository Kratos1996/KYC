package com.chaintech.kyc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.chaintech.faceliveness.di.commonModule
import com.chaintech.faceliveness.di.platformModule
import com.chaintech.kyc.core.KeepScreenOnComposable
import com.chaintech.kyc.di.appModule
import com.chaintech.kyc.di.specialModule
import com.chaintech.mrzparsersdk.tesseractOcr.AndroidAppContext
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    @OptIn(KoinInternalApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidAppContext.applicationContext = this
        startKoin {
            androidLogger(org.koin.core.logger.Level.DEBUG)  // ✅ Enable debug
            androidContext(this@MainActivity)
            modules(
                appModule(),
                specialModule(),
                commonModule(),
                platformModule()
            )
        }
        // Check module contents
        val koin = GlobalContext.get()
        println("✅ Registered definitions:")
        koin.scopeRegistry.rootScope.getAll<Any>().forEach {
            println("  - ${it::class.simpleName}")
        }
        setContent {

            App{
                finish()
            }
            KeepScreenOnComposable()
        }
    }


}