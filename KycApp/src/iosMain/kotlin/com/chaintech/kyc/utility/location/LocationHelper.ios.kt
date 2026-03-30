@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.chaintech.kyc.utility.location

import com.chaintech.kyc.utility.model.LocationData
import com.chaintech.kyc.utility.model.LocationPermissionStatus
import com.chaintech.kyc.utility.model.LocationResult
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.CoreLocation.*
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.NSObject
import platform.darwin.dispatch_after
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class, DelicateCoroutinesApi::class)
actual class LocationHelper {
    companion object {
        private val sharedLocationManager = CLLocationManager().apply {
            desiredAccuracy = kCLLocationAccuracyBest
            distanceFilter = kCLDistanceFilterNone
        }
        private val sharedGeocoder = CLGeocoder()
    }

    private val locationManager = sharedLocationManager
    private val geocoder = sharedGeocoder
    private var currentDelegate: NSObject? = null

    init {
        GlobalScope.launch(Dispatchers.Default) {
            val servicesEnabled = CLLocationManager.locationServicesEnabled()
            if (!servicesEnabled) {
                println("LocationHelper: iOS - Location services are not enabled")
            }
        }
    }

    actual suspend fun getCurrentLocation(): LocationResult = withContext(Dispatchers.Default) {
        if (!CLLocationManager.locationServicesEnabled()) {
            return@withContext LocationResult.Error("Location services are disabled", isError = true)
        }

        val authStatus = CLLocationManager.authorizationStatus()

        when (authStatus) {
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways -> {
                // We have permission, get location
                getLocationWithPermission()
            }

            kCLAuthorizationStatusNotDetermined -> {
                val permissionStatus = requestPermission()
                if (permissionStatus == LocationPermissionStatus.GRANTED) {
                    // Permission granted, now get location
                    getLocationWithPermission()
                } else {
                    LocationResult.PermissionDenied
                }
            }

            kCLAuthorizationStatusDenied,
            kCLAuthorizationStatusRestricted -> {
                LocationResult.PermissionDenied
            }

            else -> {
                LocationResult.PermissionDenied
            }
        }
    }

    private suspend fun getLocationWithPermission(): LocationResult {
        return suspendCancellableCoroutine { continuation ->
            val delegate = LocationUpdateDelegate(
                onLocationReceived = { location ->
                    handleLocationUpdate(location, continuation)
                },
                onError = { error ->
                    continuation.resume(
                        LocationResult.Error(error.localizedDescription, isError = true)
                    )
                    clearDelegate()
                }
            )

            currentDelegate = delegate

            dispatch_async(dispatch_get_main_queue()) {
                locationManager.delegate = delegate

                locationManager.startUpdatingLocation()

                // Set a timeout to stop location updates if they don't come
                dispatch_after(
                    dispatch_time(DISPATCH_TIME_NOW, 10_000_000_000), // 10 seconds
                    dispatch_get_main_queue()
                ) {
                    if (currentDelegate == delegate) {
                        locationManager.stopUpdatingLocation()
                        clearDelegate()
                        if (continuation.isActive) {
                            continuation.resume(
                                LocationResult.Error("Location request timed out", isError = true)
                            )
                        }
                    }
                }
            }

            continuation.invokeOnCancellation {
                dispatch_async(dispatch_get_main_queue()) {
                    locationManager.stopUpdatingLocation()
                    clearDelegate()
                }
            }
        }
    }

    private fun handleLocationUpdate(location: CLLocation, continuation: CancellableContinuation<LocationResult>) {
        // Stop updates immediately after getting first location
        dispatch_async(dispatch_get_main_queue()) {
            locationManager.stopUpdatingLocation()
        }

        location.coordinate.useContents {
            val lat = latitude
            val lon = longitude

            // Reverse geocode for address
            geocoder.reverseGeocodeLocation(location) { placemarks, error ->
                val placemark = placemarks?.firstOrNull() as? CLPlacemark

                val result = LocationResult.Success(
                    LocationData(
                        latitude = lat,
                        longitude = lon,
                        address = placemark?.let {
                            buildString {
                                it.subThoroughfare?.let { num -> append("$num ") }
                                it.thoroughfare?.let { street -> append("$street, ") }
                                it.locality?.let { city -> append("$city, ") }
                                it.administrativeArea?.let { state -> append("$state ") }
                                it.postalCode?.let { zip -> append(zip) }
                            }.trim().trimEnd(',')
                        },
                        city = placemark?.locality,
                        state = placemark?.administrativeArea,
                        country = placemark?.country,
                        postalCode = placemark?.postalCode
                    )
                )

                if (continuation.isActive) {
                    continuation.resume(result)
                }
                clearDelegate()
            }
        }
    }

    actual suspend fun requestPermission(): LocationPermissionStatus {
        val currentStatus = withContext(Dispatchers.Default) {
            CLLocationManager.authorizationStatus()
        }

        if (currentStatus != kCLAuthorizationStatusNotDetermined) {
            return mapAuthorizationStatus(currentStatus)
        }

        return suspendCancellableCoroutine { continuation ->
            val delegate = AuthorizationDelegate { status ->
                continuation.resume(mapAuthorizationStatus(status))
                clearDelegate()
            }

            currentDelegate = delegate

            dispatch_async(dispatch_get_main_queue()) {
                locationManager.delegate = delegate
                locationManager.requestWhenInUseAuthorization()
            }

            continuation.invokeOnCancellation {
                clearDelegate()
            }
        }
    }

    actual suspend fun checkPermission(): LocationPermissionStatus = withContext(Dispatchers.Default) {
        mapAuthorizationStatus(CLLocationManager.authorizationStatus())
    }

    actual fun openLocationSettings() {
        val settingsUrl = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
        if (settingsUrl != null && UIApplication.sharedApplication.canOpenURL(settingsUrl)) {
            dispatch_async(dispatch_get_main_queue()) {
                UIApplication.sharedApplication.openURL(settingsUrl)
            }
        }
    }

    private fun clearDelegate() {
        dispatch_async(dispatch_get_main_queue()) {
            locationManager.delegate = null
            currentDelegate = null
        }
    }

    private fun mapAuthorizationStatus(status: CLAuthorizationStatus): LocationPermissionStatus {
        return when (status) {
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways -> LocationPermissionStatus.GRANTED
            kCLAuthorizationStatusDenied -> LocationPermissionStatus.DENIED
            kCLAuthorizationStatusRestricted -> LocationPermissionStatus.RESTRICTED
            else -> LocationPermissionStatus.NOT_DETERMINED
        }
    }
}

// Location update delegate
private class LocationUpdateDelegate(
    private val onLocationReceived: (CLLocation) -> Unit,
    private val onError: (NSError) -> Unit
) : NSObject(), CLLocationManagerDelegateProtocol {

    private var hasReceivedLocation = false

    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        // Only process the first location update
        if (hasReceivedLocation) return

        val location = didUpdateLocations.firstOrNull() as? CLLocation

        location?.let {
            hasReceivedLocation = true
            onLocationReceived(it)
        }
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        onError(didFailWithError)
    }
}

// Authorization delegate
private class AuthorizationDelegate(
    private val onAuthorizationChanged: (CLAuthorizationStatus) -> Unit
) : NSObject(), CLLocationManagerDelegateProtocol {

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        val status = manager.authorizationStatus

        // Only respond if status is determined
        if (status != kCLAuthorizationStatusNotDetermined) {
            onAuthorizationChanged(status)
        }
    }

    // iOS 14+ delegate method
    override fun locationManager(manager: CLLocationManager, didChangeAuthorizationStatus: CLAuthorizationStatus) {
        if (didChangeAuthorizationStatus != kCLAuthorizationStatusNotDetermined) {
            onAuthorizationChanged(didChangeAuthorizationStatus)
        }
    }
}
