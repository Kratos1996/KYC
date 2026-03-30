package com.chaintech.kyc.network

import com.chaintech.kyc.network.api.IpApiService
import com.chaintech.kyc.utility.model.PublicIp

class IpRepository(
    private val ipApiService: IpApiService
) {

    suspend fun getIpInfo(ip: String): Result<PublicIp.IpInfo> = try {
        val ipInfo = ipApiService.fetchIpInfo(ip)
        if (ipInfo != null) {
            Result.success(ipInfo)
        } else {
            Result.failure(Exception("Failed to fetch IP information"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
    suspend fun getPublicIp(): Result<String> = try {
        val publicIp = ipApiService.getPublicIp()
        if (publicIp != null) {
            Result.success(publicIp)
        } else {
            Result.failure(Exception("Failed to fetch public IP"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun getLocalIPv6Addresses(): List<String> {
        return getIPv6AddressesPlatform()
    }
}