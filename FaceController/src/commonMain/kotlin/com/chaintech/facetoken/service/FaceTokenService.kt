package com.chaintech.facetoken.service

import androidx.compose.ui.graphics.ImageBitmap
import com.chaintech.facetoken.dataModels.FaceTokenException


interface FaceTokenService {

    suspend fun initialize()

    @Throws(FaceTokenException::class)
    fun getFaceToken(imageBitmap: ImageBitmap): FloatArray

    companion object Companion {
        fun create(): FaceTokenService {
            return FaceTokenServiceImpl()
        }
    }
}