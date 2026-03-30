import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "KycApp"
            isStatic = true
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            // Koin Core - supports all platforms
            // Koin Android - Android only
            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)
            implementation("com.google.android.gms:play-services-location:21.3.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            /* Native LifeCycle and Navigation */
            implementation(libs.androidx.lifecycle.runtimeCompose)
            // ✅ Koin Core - supports all platforms

            implementation(libs.kotlinx.serialization)
            implementation(libs.androidx.navigation.compose)
            //Lifecycle
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.viewmodelCompose)

            /* koin - dependency injection */
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.sdp.ssp.compose.multiplatform)

            /// Compose 1.9.0+
            implementation("tech.annexflow.compose:constraintlayout-compose-multiplatform:0.6.1")
            /// Compose 1.9.0+ with different tech.annexflow.constraintlayout.core package
            implementation("tech.annexflow.compose:constraintlayout-compose-multiplatform:0.6.1-shaded-core")
            /// Compose 1.9.0+ with different tech.annexflow.constraintlayout package
            implementation("tech.annexflow.compose:constraintlayout-compose-multiplatform:0.6.1-shaded")

            implementation("network.chaintech:cmp-easy-permission:1.0.3")
//            // MRZParserKit
//            implementation(project(":MRZParser"))
            // FaceController
            implementation(project(":FaceController"))
            //Document Controller
            implementation(project(":DocumentController"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.chaintech.kyc"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.chaintech.kyc"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}
