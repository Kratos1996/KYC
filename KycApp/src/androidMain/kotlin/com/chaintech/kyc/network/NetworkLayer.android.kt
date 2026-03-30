@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.chaintech.kyc.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.Inet6Address
import java.net.NetworkInterface

actual class HttpClient {

    actual suspend fun execute(request: HttpRequest): HttpResponse = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(request.url)
            connection = url.openConnection() as HttpURLConnection

            // Configure connection
            connection.requestMethod = request.method.name
            connection.connectTimeout = request.timeout.toInt()
            connection.readTimeout = request.timeout.toInt()
            connection.doInput = true

            // Set headers
            request.headers.forEach { (key, value) ->
                connection.setRequestProperty(key, value)
            }

            // Write body if present
            if (request.body != null && request.method != HttpMethod.GET) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(request.body)
                    writer.flush()
                }
            }

            // Get response code
            val statusCode = connection.responseCode

            // Read response body
            val responseBody = if (statusCode in 200..299) {
                BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    reader.readText()
                }
            } else {
                // Read error stream
                connection.errorStream?.let { errorStream ->
                    BufferedReader(InputStreamReader(errorStream)).use { reader ->
                        reader.readText()
                    }
                } ?: ""
            }

            // Get response headers
            val responseHeaders = mutableMapOf<String, String>()
            connection.headerFields.forEach { (key, values) ->
                key?.let { responseHeaders[it] = values.joinToString(", ") }
            }

            HttpResponse(
                statusCode = statusCode,
                body = responseBody,
                headers = responseHeaders
            )

        } catch (e: Exception) {
            throw NetworkException("Network request failed: ${e.message}", e)
        } finally {
            connection?.disconnect()
        }
    }

    actual fun close() {
        // No cleanup needed for HttpURLConnection
    }
}

actual fun getIPv6AddressesPlatform(): List<String> {
    val ipv6List = mutableListOf<String>()

    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        interfaces?.toList()?.forEach { networkInterface ->
            // Skip loopback and down interfaces
            if (!networkInterface.isUp || networkInterface.isLoopback) return@forEach

            val addresses = networkInterface.inetAddresses
            for (address in addresses) {
                if (address is Inet6Address && !address.isLoopbackAddress) {
                    // Remove zone index (like %wlan0)
                    val ipv6 = address.hostAddress?.split("%")?.get(0)
                    ipv6?.let {
                        ipv6List.add(it)
                    }
                }
            }
        }
    } catch (e: Exception) {
        println("Error getting IPv6 addresses: ${e.message}")
    }

    return ipv6List
}
