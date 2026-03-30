@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.chaintech.kyc.network

import kotlinx.cinterop.*
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.*
import platform.darwin.freeifaddrs
import platform.darwin.getifaddrs
import platform.darwin.ifaddrs
import platform.darwin.inet_ntop
import platform.posix.AF_INET
import platform.posix.AF_INET6
import platform.posix.NI_MAXHOST
import platform.posix.NI_NUMERICHOST
import platform.posix.getnameinfo
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual class HttpClient {

    private val session = NSURLSession.sharedSession

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    actual suspend fun execute(request: HttpRequest): HttpResponse = suspendCancellableCoroutine { continuation ->
        val url = NSURL(string = request.url)
        val urlRequest = NSMutableURLRequest(uRL = url)

        // Set HTTP method
        urlRequest.setHTTPMethod(request.method.name)
        urlRequest.setTimeoutInterval(request.timeout.toDouble() / 1000.0)

        // Set headers
        request.headers.forEach { (key, value) ->
            urlRequest.setValue(value, forHTTPHeaderField = key)
        }

        // Set body if present
        request.body?.let { body ->
            urlRequest.setHTTPBody(body.encodeToByteArray().toNSData())
            urlRequest.setValue("application/json", forHTTPHeaderField = "Content-Type")
        }

        // Create data task
        val task = session.dataTaskWithRequest(urlRequest) { data, response, error ->
            if (error != null) {
                continuation.resumeWithException(
                    NetworkException("Network request failed: ${error.localizedDescription}")
                )
                return@dataTaskWithRequest
            }

            val httpResponse = response as? NSHTTPURLResponse
            val statusCode = httpResponse?.statusCode?.toInt() ?: 0

            val responseBody = data?.let {
                NSString.create(data = it, encoding = NSUTF8StringEncoding) as String
            } ?: ""

            val responseHeaders = mutableMapOf<String, String>()

            httpResponse?.allHeaderFields?.forEach { (key, value) ->
                responseHeaders[key.toString()] = value.toString()
            }

            continuation.resume(
                HttpResponse(
                    statusCode = statusCode,
                    body = responseBody,
                    headers = responseHeaders
                )
            )
        }

        task.resume()

        continuation.invokeOnCancellation {
            task.cancel()
        }
    }

    actual fun close() {
        session.finishTasksAndInvalidate()
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData = memScoped {
    NSData.create(bytes = allocArrayOf(this@toNSData), length = this@toNSData.size.toULong())
}

@OptIn(ExperimentalForeignApi::class)
actual fun getIPv6AddressesPlatform(): List<String> {
    val ipv6List = mutableListOf<String>()
    val localIPAddressInfo = getAllLocalIPAddresses()
    val localIpv6 = localIPAddressInfo.lastOrNull{
        it.interfaceName == "en0" && it.addressType == "IPv6"
    }?.ipAddress
    localIpv6?.let{ipv6List.add(it)}

    return ipv6List
}

private data class IPAddressInfo(
    val interfaceName: String,
    val addressType: String, // "IPv4" or "IPv6"
    val ipAddress: String
)

@OptIn(ExperimentalForeignApi::class)
private fun getAllLocalIPAddresses(): List<IPAddressInfo> {
    val addresses = mutableListOf<IPAddressInfo>()

    memScoped {
        val ifaddrPtr = alloc<CPointerVar<ifaddrs>>()

        if (getifaddrs(ifaddrPtr.ptr) != 0) {
            return addresses
        }

        var currentInterface = ifaddrPtr.value

        while (currentInterface != null) {
            val interfacePointed = currentInterface.pointed
            val addrFamily = interfacePointed.ifa_addr?.pointed?.sa_family

            val name = interfacePointed.ifa_name?.toKString() ?: ""

            val addressType = when (addrFamily?.toInt()) {
                AF_INET -> "IPv4"
                AF_INET6 -> "IPv6"
                else -> null
            }

            if (addressType != null && interfacePointed.ifa_addr != null) {
                val hostname = ByteArray(NI_MAXHOST)
                val addr = interfacePointed.ifa_addr ?: return emptyList()
                hostname.usePinned { pinnedHostname ->
                    val result = getnameinfo(
                        addr,
                        addr.pointed.sa_len.toUInt(),
                        pinnedHostname.addressOf(0),
                        NI_MAXHOST.toUInt(),
                        null,
                        0u,
                        NI_NUMERICHOST
                    )

                    if (result == 0) {
                        val ipAddress = pinnedHostname.get().toKString()
                        addresses.add(
                            IPAddressInfo(
                                interfaceName = name,
                                addressType = addressType,
                                ipAddress = ipAddress
                            )
                        )
                    }
                }
            }

            currentInterface = interfacePointed.ifa_next
        }

        freeifaddrs(ifaddrPtr.value)
    }

    return addresses
}