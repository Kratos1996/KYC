package com.chaintech.kyc.utility.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object IPValues {
    var ipv6: String? = null
    var publicIp: String? = null
    var ipInfo: PublicIp.IpInfo? = null
    var lattitude: Double = 0.0
    var longnitude: Double = 0.0
}
object  SpoofScore {
    var  sScore   = 0.0f
    var  similarityScore  = 0.0f
}

object PublicIp {
    @Serializable
    data class IpInfo(
        val ip: String,
        val network: String,
        val version: String,
        val city: String,
        val region: String,
        val region_code: String,
        val country: String,
        val country_name: String,
        val country_code: String,
        val country_code_iso3: String,
        val country_capital: String,
        val country_tld: String,
        val continent_code: String,
        val in_eu: Boolean,
        val postal: String,
        val latitude: Double,
        val longitude: Double,
        val timezone: String,
        val utc_offset: String,
        val country_calling_code: String,
        val currency: String,
        val currency_name: String,
        val languages: String,
        val country_area: Double,
        val country_population: Long,
        val asn: String,
        val org: String
    )

    object FlexibleIntSerializer : KSerializer<Int> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("FlexibleInt", PrimitiveKind.INT)

        override fun serialize(encoder: Encoder, value: Int) {
            encoder.encodeInt(value)
        }

        override fun deserialize(decoder: Decoder): Int {
            return try {
                decoder.decodeInt()
            } catch (e: Exception) {
                // If it fails as Int, try as Double and convert
                decoder.decodeDouble().toInt()
            }
        }
    }
}
