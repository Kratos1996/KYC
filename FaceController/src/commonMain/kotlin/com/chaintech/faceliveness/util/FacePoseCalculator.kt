@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.chaintech.faceliveness.util

expect class FacePoseCalculator {
    fun calculatePose(result: Any): FacePosition
    fun release()
}