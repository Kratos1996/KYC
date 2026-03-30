@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.chaintech.kyc.utility.location

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.chaintech.kyc.utility.model.LocationData
import com.chaintech.kyc.utility.model.LocationPermissionStatus
import com.chaintech.kyc.utility.model.LocationResult
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.Locale
import kotlin.coroutines.resume

actual class LocationHelper(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val geocoder: Geocoder = Geocoder(context, Locale.getDefault())

    actual suspend fun getCurrentLocation(): LocationResult {
        return try {
            // Check permission first
            if (checkPermission() != LocationPermissionStatus.GRANTED) {
                return LocationResult.PermissionDenied
            }

            // Check if location is enabled
            if (!isLocationEnabled()) {
                return LocationResult.LocationDisabled
            }

            // Get current location
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).await()

            if (location == null) {
                return LocationResult.Error("Unable to get location" , isError = true)
            }

            // Get address from coordinates
            val address = getAddressFromCoordinates(
                location.latitude,
                location.longitude
            )

            LocationResult.Success(
                LocationData(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    address = address?.getAddressLine(0),
                    city = address?.locality,
                    state = address?.adminArea,
                    country = address?.countryName,
                    postalCode = address?.postalCode
                )
            )
        } catch (e: SecurityException) {
            LocationResult.PermissionDenied
        } catch (e: Exception) {
            LocationResult.Error(e.message ?: "Unknown error occurred" , true)
        }
    }

    actual suspend fun requestPermission(): LocationPermissionStatus {
        // This should be called from Activity/Fragment with requestPermissions
        // Here we just check current status
        return checkPermission()
    }

    actual suspend fun checkPermission(): LocationPermissionStatus {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return when {
            fineLocationGranted || coarseLocationGranted -> LocationPermissionStatus.GRANTED
            else -> LocationPermissionStatus.DENIED
        }
    }

    actual fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private suspend fun getAddressFromCoordinates(
        latitude: Double,
        longitude: Double
    ): Address? = suspendCancellableCoroutine { continuation ->
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                    continuation.resume(addresses.firstOrNull())
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                continuation.resume(addresses?.firstOrNull())
            }
        } catch (e: Exception) {
            continuation.resume(null)
        }
    }
}
