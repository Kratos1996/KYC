@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.chaintech.kyc.utility.location

import com.chaintech.kyc.utility.model.LocationPermissionStatus
import com.chaintech.kyc.utility.model.LocationResult


expect class LocationHelper {
    suspend fun getCurrentLocation(): LocationResult
    suspend fun requestPermission(): LocationPermissionStatus
    suspend fun checkPermission(): LocationPermissionStatus
    fun openLocationSettings()
}
