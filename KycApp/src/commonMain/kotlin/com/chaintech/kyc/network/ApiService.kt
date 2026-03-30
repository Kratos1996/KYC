package com.chaintech.kyc.network

import kotlinx.serialization.json.Json

class ApiService( val baseUrl: String,val httpClient:HttpClient) {

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // GET request with full URL (for external APIs)
    suspend inline fun <reified T> getExternal(
        fullUrl: String,
        headers: Map<String, String> = emptyMap()
    ): T {
        val request = HttpRequest(
            url = fullUrl,
            method = HttpMethod.GET,
            headers = headers
        )

        val response = httpClient.execute(request)

        if (!response.isSuccessful) {
            throw NetworkException("HTTP ${response.statusCode}: ${response.body}")
        }

        return json.decodeFromString(response.body)
    }

    // GET request with base URL
    suspend inline fun <reified T> get(
        endpoint: String,
        headers: Map<String, String> = emptyMap()
    ): T {
        val request = HttpRequest(
            url = "$baseUrl$endpoint",
            method = HttpMethod.GET,
            headers = headers
        )

        val response = httpClient.execute(request)

        if (!response.isSuccessful) {
            throw NetworkException("HTTP ${response.statusCode}: ${response.body}")
        }

        return json.decodeFromString(response.body)
    }

    suspend inline fun <reified T, reified R> post(
        endpoint: String,
        body: T,
        headers: Map<String, String> = emptyMap()
    ): R {
        val jsonBody = json.encodeToString(body)

        val request = HttpRequest(
            url = "$baseUrl$endpoint",
            method = HttpMethod.POST,
            headers = headers + ("Content-Type" to "application/json"),
            body = jsonBody
        )

        val response = httpClient.execute(request)

        if (!response.isSuccessful) {
            throw NetworkException("HTTP ${response.statusCode}: ${response.body}")
        }

        return json.decodeFromString(response.body)
    }

    suspend inline fun <reified R> postNoBody(
        endpoint: String,
        headers: Map<String, String> = emptyMap()
    ): R {
        val request = HttpRequest(
            url = "$baseUrl$endpoint",
            method = HttpMethod.POST,
            headers = headers
        )

        val response = httpClient.execute(request)

        if (!response.isSuccessful) {
            throw NetworkException("HTTP ${response.statusCode}: ${response.body}")
        }

        return json.decodeFromString(response.body)
    }

    fun close() {
        httpClient.close()
    }
}
