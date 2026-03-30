import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_1_8)
                }
            }
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    
    sourceSets {
        commonMain.dependencies {
            //put your multiplatform dependencies here
            implementation(compose.components.resources)
            implementation(compose.runtime)
            implementation(compose.material3)
            implementation(compose.ui)
            api(libs.cmp.file.picker)
            // TensorflowLiteRunner
            implementation(project(":TensorflowLiteRunner"))
        }

        androidMain.dependencies {
            implementation(libs.androidx.camera.core)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)
            implementation(libs.accompanist.permissions)
            api(libs.androidx.core.ktx)
            api(libs.guava)
        }
    }
}

android {
    namespace = "com.chaintech.documentcontroller"
    compileSdk = 36
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        mlModelBinding = true
    }
}