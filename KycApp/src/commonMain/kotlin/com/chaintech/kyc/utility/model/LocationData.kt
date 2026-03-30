package com.chaintech.kyc.utility.model


import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Serializable
data class LocationData @OptIn(ExperimentalTime::class) constructor(
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val postalCode: String? = null,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
)

sealed class LocationResult {
    data class Success(val location: LocationData) : LocationResult()
    data class Error(val message: String , val isError: Boolean) : LocationResult()
    data object PermissionDenied : LocationResult()
    data object LocationDisabled : LocationResult()
}


data class LocationState(
    val location: LocationData? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val errorLocation: Boolean = false,
    val permissionDenied: Boolean = false,
    val locationDisabled: Boolean = false
)

enum class LocationPermissionStatus {
    GRANTED,
    DENIED,
    NOT_DETERMINED,
    RESTRICTED
}
