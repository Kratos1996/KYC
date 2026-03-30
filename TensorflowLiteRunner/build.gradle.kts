import org.gradle.internal.impldep.org.junit.experimental.categories.Categories.CategoryFilter.exclude
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.android.library)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.mavenPublish)
}

val xcfName = "TensorflowLiteRunner"
val bundleId = "com.chaintech.tensorflowrunner"

kotlin {
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

    // iOS targets
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = xcfName
            isStatic = false
            freeCompilerArgs += "-Xbinary=bundleId=$bundleId"
        }
    }

    cocoapods {
        version = "0.0.1"
        summary = "Shared KMP module using TensorFlow Lite C"
        homepage = "https://demotest.com/"
        ios.deploymentTarget = "13.0"

        pod("TensorFlowLiteObjC", moduleName = "TFLTensorFlowLite")

        framework {
            baseName = xcfName
            isStatic = false
            linkerOpts(
                project.file("../iosApp/Pods/TensorFlowLiteC/Frameworks").path.let { "-F$it" },
                "-framework", "TensorFlowLiteC"
            )
        }

        // Point to the iOS app Podfile
        // This ensures both Gradle and Xcode use the same pod setup
        podfile = project.file("../iosApp/Podfile")
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.components.resources)
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlinx.coroutines.core)

                implementation(compose.ui)
            }
        }

        androidMain {
            dependencies {
                api(libs.tensorflow.lite)
            }
        }

        iosMain {
            dependencies {
            }
        }
    }
}

android {
    namespace = bundleId
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }

    buildFeatures {
        compose = true
    }
}

//// 🔥 CRITICAL: Make podInstall run before any iOS framework linking
//tasks.named("podInstall").configure {
//    doFirst {
//        println("📦 Installing CocoaPods dependencies...")
//    }
//    doLast {
//        println("✅ CocoaPods installed successfully")
//    }
//}
//
//// Ensure linkPod tasks depend on podInstall
//tasks.matching {
//    it.name.startsWith("linkPod") && it.name.contains("FrameworkIos")
//}.configureEach {
//    dependsOn("podInstall")
//}