package com.chaintech.kyc.utility.location

import com.chaintech.kyc.utility.model.LocationResult


class LocationRepository(
    private val locationHelper: LocationHelper
) {

    suspend fun getCurrentLocation(): LocationResult {
        return locationHelper.getCurrentLocation()
    }

    suspend fun requestLocationPermission() = locationHelper.requestPermission()

    suspend fun checkLocationPermission() = locationHelper.checkPermission()

    fun openSettings() = locationHelper.openLocationSettings()
}
