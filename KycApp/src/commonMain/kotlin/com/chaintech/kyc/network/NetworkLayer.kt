@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.chaintech.kyc.network

import com.chaintech.kyc.utility.model.PublicIp
import kotlinx.serialization.Serializable

// HTTP Method enum
enum class HttpMethod {
    GET, POST, PUT, DELETE, PATCH
}

// Request configuration
data class HttpRequest(
    val url: String,
    val method: HttpMethod = HttpMethod.GET,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
    val timeout: Long = 30000L // 30 seconds
)

// Response wrapper
@Serializable
data class HttpResponse(
    val statusCode: Int,
    val body: String,
    val headers: Map<String, String> = emptyMap(),
    val isSuccessful: Boolean = statusCode in 200..299
)

// Exception class
class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)

data class IpInfoState(
    val ipInfo: PublicIp.IpInfo? = null,
    val ipv6Addresses: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

// Expect HTTP client
expect class HttpClient() {
    suspend fun execute(request: HttpRequest): HttpResponse
    fun close()
}
// Platform-specific IPv6 implementation
expect fun getIPv6AddressesPlatform(): List<String>
