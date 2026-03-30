package com.chaintech.kyc.network.api

import com.chaintech.kyc.network.*
import com.chaintech.kyc.utility.model.PublicIp
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

class IpApiService(
    private val apiService: ApiService
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }
    // Get public IP from ipify
    suspend fun getPublicIp(): String? {
        return try {
            val request = HttpRequest(
                url = "https://api.ipify.org",
                method = HttpMethod.GET,
                timeout = 10000L
            )

            val response = apiService.httpClient.execute(request)

            if (response.isSuccessful) {
                response.body.trim()
            } else {
                null
            }
        } catch (e: Exception) {
            println("Error fetching public IP: ${e.message}")
            null
        }
    }

    suspend fun fetchIpInfo(ip: String, maxRetries: Int = 5): PublicIp.IpInfo? {
        var attempt = 0
        var delayMillis = 1000L

        while (attempt < maxRetries) {
            try {
                val headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36",
                    "Accept" to "application/json, text/plain, */*",
                    "Accept-Language" to "en-US,en;q=0.9",
                    "Connection" to "keep-alive",
                    "Referer" to "https://ipapi.co/",
                    "Origin" to "https://ipapi.co",
                    "Sec-Fetch-Mode" to "cors",
                    "Sec-Fetch-Site" to "same-origin"
                )

                // Using ApiService.get with custom base URL override
                val request = HttpRequest(
                    url = "${apiService.baseUrl}$ip/json/",
                    method = HttpMethod.GET,
                    headers = headers,
                    timeout = 30000L
                )

                val response = apiService.httpClient.execute(request)

                when (response.statusCode) {
                    200 -> {
                        val data = json.decodeFromString<PublicIp.IpInfo>(response.body)
                        return data
                    }
                    429 -> {
                        println("Rate limited. Retrying in ${delayMillis}ms...")
                        attempt++
                        delay(delayMillis)
                        delayMillis *= 2 // exponential backoff
                    }
                    else -> {
                        println("Unexpected response code: ${response.statusCode}")
                        return null
                    }
                }
            } catch (e: Exception) {
                println("Network error: ${e.message}")
                return null
            }
        }

        println("Max retries exceeded")
        return null
    }
}
