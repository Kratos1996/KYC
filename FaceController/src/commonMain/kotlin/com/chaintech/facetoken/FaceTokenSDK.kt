package com.chaintech.facetoken

import com.chaintech.facetoken.service.FaceTokenService

object FaceTokenSDK {
    fun createFaceTokenService(): FaceTokenService {
        return FaceTokenService.create()
    }
}