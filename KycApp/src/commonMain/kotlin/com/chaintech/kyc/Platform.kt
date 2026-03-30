package com.chaintech.kyc

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform