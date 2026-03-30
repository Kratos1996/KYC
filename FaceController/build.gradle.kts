import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.android.library)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinx.serialization)
}

val xcfName = "FaceController"
val bundleId = "com.chaintech.facecontroller"

kotlin {
    applyDefaultHierarchyTemplate()

    androidTarget {
        publishLibraryVariants("release", "debug")
        compilations.all {
            compileTaskProvider {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                    freeCompilerArgs.add("-Xjdk-release=${JavaVersion.VERSION_17}")
                }
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = xcfName
            isStatic = true
            freeCompilerArgs += "-Xbinary=bundleId=$bundleId"

        }
        iosTarget.compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.addAll(
                        "-Xopt-in=kotlinx.cinterop.ExperimentalForeignApi",
                        "-Xopt-in=kotlinx.cinterop.BetaInteropApi"
                    )
                }
            }
        }
    }

    cocoapods {
        version = "0.0.1"
        summary = "Face Liveness Detection"
        homepage = "https://github.com/ctn-mobile-innovation/KMP-ml/"
        ios.deploymentTarget = "13.0"

        pod("MediaPipeTasksVision") {
            moduleName = "MediaPipeTasksVision"
            extraOpts += listOf("-compiler-option", "-fmodules")
        }

        // GoogleMLKit - OCR
        pod("MLKitTextRecognition")
        pod("MLKitVision")
        pod("MLKitCommon")

        // TesseractOCR
        pod("kta-tesseract", moduleName = "TesseractOCR")
//        pod("TesseractOCRiOS", moduleName = "TesseractOCR")

        framework {
            baseName = xcfName
            isStatic = true
        }
        podfile = project.file("../iosApp/Podfile")
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.components.resources)
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.coroutines.core)

                // Compose - supports all platforms
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)

                implementation(libs.kotlinx.serialization)

                implementation(libs.androidx.navigation.compose)

                // Koin Core - supports all platforms
                implementation(libs.koin.core)
                implementation(libs.koin.compose)

                implementation(libs.androidx.lifecycle.viewmodelCompose)

                // TensorflowLiteRunner
                implementation(project(":TensorflowLiteRunner"))

                // kotlinx-datetime
                implementation(libs.kotlinx.datetime)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
                // Compose Android specific
                implementation(compose.preview)

                // ✅ These are Android-only, so they go here
                implementation(libs.androidx.activity.compose)

                // CameraX - Android only
                implementation(libs.androidx.camera.camera2)
                implementation(libs.androidx.camera.lifecycle)
                implementation(libs.androidx.camera.view)
                implementation(libs.androidx.camera.video)

                // MediaPipe - Android only
                implementation(libs.tasks.vision)

                // Koin Android - Android only
                implementation(libs.koin.android)
                implementation(libs.koin.androidx.compose)

                // GoogleMLKit - OCR
                implementation(libs.text.recognition)

                // TesseractOCR
                implementation(libs.tesseract4android)
            }
        }

        iosMain {
            dependencies {
                // Add iOS-specific dependencies here. This a source set created by Kotlin Gradle
                // Plugin (KGP) that each specific iOS target (e.g., iosX64) depends on as
                // part of KMP’s default source set hierarchy. Note that this source set depends
                // on common by default and will correctly pull the iOS artifacts of any
                // KMP dependencies declared in commonMain.
            }
        }
    }

}

// Target declarations - add or remove as needed below. These define
// which platforms this KMP module supports.
// See: https://kotlinlang.org/docs/multiplatform-discover-project.html#targets
android {
    namespace = bundleId
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }
    sourceSets["main"].jniLibs.srcDirs("src/main/jniLibs")
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    // ✅ Add packaging options to avoid conflicts
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/versions/**"
        }
    }

}