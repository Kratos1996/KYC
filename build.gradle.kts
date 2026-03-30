plugins {
    alias(libs.plugins.kotlinMultiplatform).apply(false)
    alias(libs.plugins.composeMultiplatform).apply(false)
    alias(libs.plugins.android.application).apply(false)
    alias(libs.plugins.android.library).apply(false)
    alias(libs.plugins.mavenPublish).apply(false)
    alias(libs.plugins.compose.compiler).apply(false)
    alias(libs.plugins.kotlinCocoapods).apply(false)
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false

    alias(libs.plugins.android.lint) apply false
    alias(libs.plugins.kotlinx.serialization).apply(false)
}